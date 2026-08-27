#!/usr/bin/perl
# serve.pl - minimal static HTTP server, zero dependencies (perl core only).
# Drop-in replacement for `python3 -m http.server` when python3 is not
# installed on the Mac. macOS always ships /usr/bin/perl.
#
# Usage:  perl serve.pl <port> <directory> [bind-ip]
#
# One file per request, Content-Length set, connection closed after each
# response (same behavior the car's curl expects). Serves binary files
# correctly (binmode on both ends).
use strict;
use warnings;
use IO::Socket::INET;

my ($port, $root, $bind) = @ARGV;
$port //= 8123;
$root //= '.';
$bind //= '0.0.0.0';
die "serve.pl: no such directory: $root\n" unless -d $root;

my $srv = IO::Socket::INET->new(
  LocalAddr => $bind, LocalPort => $port, ReuseAddr => 1,
  Listen => 32, Proto => 'tcp',
) or die "serve.pl: cannot listen on $bind:$port: $!\n";
print STDERR "serve.pl: serving $root on $bind:$port\n";

$SIG{INT}  = sub { exit 0 };
$SIG{TERM} = sub { exit 0 };

while (1) {
  my $cli = $srv->accept();
  next unless defined $cli;
  binmode($cli);
  my $req = <$cli>;
  next unless defined $req;
  my ($path) = $req =~ m{^GET\s+(\S+)};
  next unless defined $path;
  $path =~ s{\?.*}{};
  next if $path =~ m{\.\.};        # no path traversal, ever
  $path = '/index.html' if $path eq '/';

  my $file = $root . $path;
  if (-f $file) {
    open my $fh, '<', $file or next;
    binmode($fh);
    my $len = -s $file;
    print $cli "HTTP/1.1 200 OK\r\nContent-Length: $len\r\nConnection: close\r\n\r\n";
    my $buf;
    while (read($fh, $buf, 65536)) { print $cli $buf; }
    close $fh;
  } else {
    print $cli "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
  }
  close $cli;
}
