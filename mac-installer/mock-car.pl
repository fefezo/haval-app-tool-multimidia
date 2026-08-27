#!/usr/bin/perl
# mock-car.pl - SIMULATED Haval H6 head unit for offline dry-runs.
# A fake telnet daemon (127.0.0.1:2323 by default) that mimics the REAL device's
# behavior closely enough to exercise the installer's new failure/retry paths:
#
#   * echoes each command line (":/ # <cmd>") like the real shell, then answers
#   * shizuku pm install is BLOCKED by name until the frida hook has loaded
#     ("beantechs disallow apk moe.shizuku.privileged.api") - exactly like this
#     unit - so MODE A fails and MODE B is forced
#   * the FIRST fridainject always fails (frida attach error) so the hook-retry
#     path runs; the second one succeeds and prints [HAVAL-HOOK-READY]
#   * each file's size check fails the FIRST time it is seen (per run) so the
#     transfer-retry path runs; later checks pass
#
# State persists across telnet connections in a temp file (each run_car opens a
# new connection), so the flow works as one continuous install session.
#
# Usage:  perl mock-car.pl [port]
#
# Replies:
#   echo __HAVX_n__          -> echoes the marker (client sync)
#   curl ...                 -> fake download completed
#   pm install shizuku (no hook) -> Failure (beantechs disallow...)
#   pm install shizuku (hook)    -> Success
#   pm install haval          -> Success
#   dumpsys package ...      -> userId=10000 (low UID, telnet-at-boot will work)
#   pm list packages ...     -> haval always; shizuku only after it "installed"
#   pidof/pgrep              -> 1234 (system_server pid)
#   fridainject (1st)        -> {"type":"error",...}   (mock: attach failure)
#   fridainject (2nd+)       -> [HAVAL-HOOK-READY] ...
#   setsid                   -> [1] 6372
#   anything else            -> nothing (exit 0), then a shell prompt
use strict;
use warnings;
use IO::Socket::INET;
use File::Spec;

my $port = shift || 2323;
my $srv = IO::Socket::INET->new(
  LocalAddr => '127.0.0.1', LocalPort => $port, ReuseAddr => 1,
  Listen => 8, Proto => 'tcp',
) or die "mock-car.pl: cannot listen on 127.0.0.1:$port: $!\n";
print STDERR "mock-car.pl: fake head unit ready on 127.0.0.1:$port (MOCK - not a real device)\n";

# fresh run state (truncate) - each installer run starts clean
my $state_file = File::Spec->catfile(File::Spec->tmpdir, "mock-haval-state-$port.txt");
open my $sf, '>', $state_file or die "cannot write $state_file: $!\n";
print $sf "inject_ok=0\nshizuku_installed=0\nseen=\n";
close $sf;

$SIG{INT}  = sub { unlink $state_file; exit 0 };
$SIG{TERM} = sub { unlink $state_file; exit 0 };

while (1) {
  my $cli = $srv->accept() or next;
  handle($cli);
}

sub load_state {
  my %st = (inject_ok => 0, shizuku_installed => 0, seen => {});
  open my $fh, '<', $state_file or return \%st;
  while (<$fh>) {
    chomp;
    if    (/^inject_ok=(\d)/)        { $st{inject_ok} = $1; }
    elsif (/^shizuku_installed=(\d)/){ $st{shizuku_installed} = $1; }
    elsif (/^seen=(.*)$/)            { $st{seen} = { map { $_ => 1 } grep { length } split /\s+/, $1 }; }
  }
  close $fh;
  return \%st;
}

sub save_state {
  my ($st) = @_;
  open my $fh, '>', $state_file or return;
  print $fh "inject_ok=$st->{inject_ok}\n";
  print $fh "shizuku_installed=$st->{shizuku_installed}\n";
  print $fh "seen=" . join(' ', sort keys %{ $st->{seen} }) . "\n";
  close $fh;
}

sub handle {
  my ($cli) = @_;
  $cli->autoflush(1);
  binmode($cli);
  # telnet negotiation offers (IAC WILL ECHO, IAC WILL SGA) - the client
  # answers WONT/DONT and we leave echo off
  print $cli "\xff\xfb\x01\xff\xfb\x03";
  print $cli "MOCK-HAVAL-H6 head unit v1.0 (simulated)\r\n";
  my $st = load_state();
  my $SYSTEM_PID = '';
  while (my $line = <$cli>) {
    $line =~ s/\r?\n$//;
    # strip any telnet negotiation bytes the client sent our way:
    #   IAC IAC           = 2 bytes
    #   IAC WILL/WONT/DO/DONT + option = 3 bytes (the option byte MUST go too,
    #   else it glues itself to the first command line and breaks matching)
    $line =~ s/\xff\xff//g;
    while ($line =~ /\xff[\xfb\xfc\xfd\xfe]/) { $line =~ s/\xff[\xfb\xfc\xfd\xfe].//; }
    $line =~ s/\xff//g;
    next unless length $line;

    print $cli ":/ # $line\r\n";   # a real shell echoes the command line first

    if ($line =~ /^echo (__HAVX_\d+__)$/) {
      # client sync marker - answer with the marker alone on its own line
      print $cli "$1\r\n";
    } elsif ($line =~ /&& echo "SIZE-(OK|BAD) (\S+)"/) {
      # installer's transfer check: `test $(wc -c < ...) -eq N && echo "SIZE-OK f" || echo "SIZE-BAD f"`
      my $f = $2;
      if ($st->{seen}{$f}) {
        print $cli "SIZE-OK $f\r\n";
      } else {
        $st->{seen}{$f} = 1; save_state($st);
        print $cli "SIZE-BAD $f\r\n";   # first time in this run: fail, so the installer retries
      }
    } elsif ($line =~ /\bpidof\b/) {
      $SYSTEM_PID = '1234';
      print $cli "$SYSTEM_PID\r\n";
    } elsif ($line =~ /^pgrep /) {
      $SYSTEM_PID = '1234' if $line =~ /system_server/;
      print $cli "$SYSTEM_PID\r\n";
    } elsif ($line =~ /^setsid /) {
      print $cli "[1] 6372\r\n";
    } elsif ($line =~ /fridainject -p /) {   # the actual inject invocation (not pkill/size-check lines)
      print $cli "[1] 6380\r\n";
      if ($st->{inject_ok}) {
        print $cli "[HAVAL-HOOK-READY] Java bridge up - hooks installing\r\n";
      } else {
        $st->{inject_ok} = 1; save_state($st);
        print $cli "{\"type\":\"error\",\"description\":\"unable to connect to remote frida-server (mock: first attach always fails)\",\"line\":0,\"column\":0}\r\n";
      }
    } elsif ($line =~ /^pm install/) {
      if ($line =~ /shizuku\.apk/ && !$st->{inject_ok}) {
        # exactly what this head unit does without the hook
        print $cli "Failure [INSTALL_FAILED_INVALID_APK: beantechs disallow apk moe.shizuku.privileged.api]\r\n";
      } else {
        $st->{shizuku_installed} = 1 if $line =~ /shizuku\.apk/;
        save_state($st);
        print $cli "Success\r\n";
      }
    } elsif ($line =~ /dumpsys package/) {
      print $cli "  userId=10000\r\n";
    } elsif ($line =~ /pm list packages/) {
      print $cli "package:br.com.redesurftank.havalshisuku\r\n";
      print $cli "package:moe.shizuku.privileged.api\r\n" if $st->{shizuku_installed};
    } elsif ($line =~ /^echo (.*)$/) {
      # generic echo (e.g. the installer's "[i] system_server pid: ...")
      my $rest = $1;
      $rest =~ s/^"//; $rest =~ s/"$//;
      $rest =~ s/\$\{SYSTEM_PID:-(.*?)\}/$SYSTEM_PID || $1/ge;
      print $cli "$rest\r\n";
    } elsif ($line =~ /curl /) {
      print $cli "mock: download complete\r\n";
    } elsif ($line =~ /^pkill|^mkdir|^chmod|^rm |^sleep |^case |^\[ -n/) {
      # silently succeeds (exit 0), like the real shell
    } else {
      print $cli "\r\n";
    }
    print $cli ":/ # \r\n";   # shell prompt after each command, like the real unit
  }
  close $cli;
}
