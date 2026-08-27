#!/usr/bin/perl
# car.pl - zero-dependency telnet client for the Haval H6 head unit (port 23).
# Drop-in replacement for car.py when python3 is not installed on the Mac.
# Uses only perl core modules (IO::Socket::INET, IO::Select).
#
# Usage:  perl car.pl <host> <commands-file>     (port via HAVAL_PORT env, default 23)
#
# Same contract as car.py: sends each command followed by a unique echo marker
# (__HAVX_<n>__), reads until the marker comes back, prints the captured
# output. Telnet IAC sequences are stripped from the stream and WILL/DO
# negotiations are answered with WONT/DONT (standard minimal client).
use strict;
use warnings;
use IO::Socket::INET;
use IO::Select;

my ($host, $cmds_file) = @ARGV;
die "usage: $0 <host> <commands-file>  (HAVAL_PORT env overrides the port)\n"
  if !defined $host || !defined $cmds_file;

my $PORT          = $ENV{HAVAL_PORT} || 23;
my $IDLE_READ_S   = 1.0;
my $DEFAULT_TO    = 120;   # seconds per command
my $SLOW_TO       = 900;   # curl of a 115 MB APK / pm install get extra time

my $sock = IO::Socket::INET->new(
  PeerAddr => $host, PeerPort => $PORT, Proto => 'tcp', Timeout => 15,
) or die "car.pl: cannot connect to $host:$PORT: $!\n";
$sock->autoflush(1);
binmode($sock);
my $sel = IO::Select->new($sock);

open my $fh, '<', $cmds_file or die "car.pl: cannot read $cmds_file: $!\n";
my @cmds = grep { /\S/ && !/^\s*#/ } <$fh>;
close $fh;
die "car.pl: no commands in $cmds_file\n" unless @cmds;
s/\r?\n$// for @cmds;   # strip line endings (car.py does the same)

# ---- telnet IAC handling -------------------------------------------------
# strip IAC sequences from incoming bytes; negotiation replies accumulate in
# $iac_reply and are flushed to the socket after each read.
my $iac_reply = '';
sub clean {
  my ($data) = @_;
  my $out = '';
  while (length $data) {
    my $b = ord(substr($data, 0, 1));
    if ($b != 0xFF) { $out .= substr($data, 0, 1, ''); next; }
    return $out . $data if length($data) < 2;            # truncated IAC
    my $cmd = ord(substr($data, 1, 1));
    if ($cmd == 0xFF) { $out .= "\xff"; substr($data, 0, 2, ''); next; }   # IAC IAC
    if ($cmd == 0xFA) {                                   # subnegotiation
      my $i = index($data, "\xff\xf0", 2);
      return $out if $i < 0;                              # wait for IAC SE
      substr($data, 0, $i + 2, '');
      next;
    }
    return $out . $data if length($data) < 3;             # truncated command
    my $opt = ord(substr($data, 2, 1));
    $iac_reply .= "\xff\xfc$opt" if $cmd == 0xFD;         # DO   -> WONT
    $iac_reply .= "\xff\xfe$opt" if $cmd == 0xFB;         # WILL -> DONT
    substr($data, 0, 3, '');
  }
  return $out;
}

sub flush_iac {
  return unless length $iac_reply;
  syswrite($sock, $iac_reply);
  $iac_reply = '';
}

sub recv_until {
  my ($timeout, $marker, $buf_ref) = @_;
  my $deadline = time() + $timeout;
  while (time() < $deadline) {
    my @r = $sel->can_read($IDLE_READ_S);
    last unless @r;
    my $n = sysread($sock, my $chunk, 65536);
    if (!defined $n || $n == 0) { print "!! connection closed by head unit\n"; exit 1; }
    $$buf_ref .= clean($chunk);
    flush_iac();
    # The head unit echoes the command line, so "echo __HAVX_n__" appears in
    # the stream before its output does. Matching the bare marker would return
    # on the ECHOED line and leave the real marker output in the socket,
    # polluting the next command. Match only when the marker starts a line
    # (that is the marker OUTPUT line) and trim the marker out of the buffer.
    if ($marker ne '' && $$buf_ref =~ /(?:\A|\n)\Q$marker\E/) {
      substr($$buf_ref, $+[0] - length($marker), length($marker)) = '';
      return 1;
    }
  }
  return 0;
}

# ---- banner: read until the socket goes quiet (max 10 s) ------------------
my $banner = '';
recv_until(10, '', \$banner);
flush_iac();
print "=== banner ===\n";
$banner =~ s/^\s+|\s+$//g;
print "$banner\n" if length $banner;

# ---- commands -------------------------------------------------------------
for my $n (0 .. $#cmds) {
  my $cmd    = $cmds[$n];
  my $marker = "__HAVX_${n}__";
  my $slow   = $cmd =~ /curl|pm install/;
  my $timeout = $slow ? $SLOW_TO : $DEFAULT_TO;
  print "=== \$ $cmd (timeout ${timeout}s) ===\n";

  my $sent = syswrite($sock, "$cmd\n");
  if (!defined $sent) { print "!! connection closed by head unit\n"; exit 1; }
  syswrite($sock, "echo $marker\n");

  my $buf = '';
  my $got = recv_until($timeout, $marker, \$buf);
  flush_iac();
  if (!$got) { print "!! TIMEOUT waiting for completion of: $cmd\n"; exit 2; }
  print $buf;   # marker line already trimmed out by recv_until
}
close $sock;
print "=== done ===\n";
