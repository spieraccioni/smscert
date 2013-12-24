#!/usr/bin/perl

use strict;
use warnings;

use constant LOG_DIR    => '/var/log';
use constant LOG_FILE   => 'checksms.log';
use constant PIDDIR     => '/var/run';

use Proc::PID::File;
use Proc::Daemon;
use Log::Dispatch;
use Log::Dispatch::File;
use Date::Format;
use File::Spec;
use File::Copy;
use DBI;
#use Net::FTP;
use File::Listing qw(parse_dir);
use Config::IniFiles;
use Digest::SHA1 qw(sha1_hex);

# Generic Parameter
my $polling_second      = 5;
my $log_level='warning';

# Connection parameters
my $user= "root";
my $password= "lore";
my $host= "localhost";
my $db = "radius";

# FTP Parameters
my $DIR_INPUT   = '/smsin';
my $DIR_OUTPUT  = '/smsout';
my $LOCAL_PATH  = '/var/spool/sms/';

my $from_number = "+393346245245";
my $template_text = "COMUNE DI LIVORNO - Il tuo account è composto da username <number> e password <password>.";
my $template_password = "";
my $template_service = "";
my $match_text = "wifi";
my $MaxDailySession = "7200";

#Parse INI file
my $cfg = new Config::IniFiles( -file => "/etc/checksms.conf" ) or die;

# Connect to the database, (the directory containing our csv file(s))
my $dbh_csv = DBI->connect("DBI:CSV:f_dir=/etc;csv_eol=\n;");

# Associate our csv file with the table name 'prospects'
$dbh_csv->{'csv_tables'}->{'type_account_table'} = { 'file' => 'type_account.csv'};


#
# fork and background process
#
our $ME = $0; $ME =~ s|.*/||;
our $PIDFILE = PIDDIR."/$ME.pid";

Proc::Daemon::Init;

# If already running, then exit
if (Proc::PID::File->running()) {
        #$log->error("Daemon already running. Exit.");
    exit(0);
    }

$polling_second = $cfg->val( 'GENERIC', 'polling_second');
$log_level              = $cfg->val( 'GENERIC', 'log_level');

$user           = $cfg->val( 'DB', 'user');
$password       = $cfg->val( 'DB', 'password');
$host           = $cfg->val( 'DB', 'host');
$db             = $cfg->val( 'DB', 'db');

$DIR_INPUT      = $cfg->val( 'FTP', 'DIR_INPUT');
$DIR_OUTPUT     = $cfg->val( 'FTP', 'DIR_OUTPUT');
$LOCAL_PATH     = $cfg->val( 'FTP', 'LOCAL_PATH');

$from_number    = $cfg->val( 'SMS', 'from_number');
$template_text  = $cfg->val( 'SMS', 'template_text');
$template_password = $cfg->val( 'SMS', 'template_password');
$template_service = $cfg->val( 'SMS', 'template_service');
$match_text     = $cfg->val( 'SMS', 'match_text');

$MaxDailySession        = $cfg->val( 'DB', 'MaxDailySession');

#
# Setup a logging agent
#
my $log = new Log::Dispatch(
      callbacks => sub { my %h=@_; return "[".$h{level}."] ".Date::Format::time2str('%Y%m%d %T', time)." $0\[$$]: ".$h{message}."\n"; }
);
$log->add( Log::Dispatch::File->new( name      => 'file1',
                                     min_level => $log_level,
                                     mode      => 'append',
                                     filename  => File::Spec->catfile(LOG_DIR, LOG_FILE),
                                   )
);
$log->warning("Starting Processing:  ".time());

$log->warning("Polling second: $polling_second ");

#
# Setup signal handlers so that we have time to cleanup before shutting down
#
my $keep_going = 1;
$SIG{HUP}  = sub { $log->warning("Caught SIGHUP:  exiting gracefully"); $keep_going = 0; };
$SIG{INT}  = sub { $log->warning("Caught SIGINT:  exiting gracefully"); $keep_going = 0; };
$SIG{QUIT} = sub { $log->warning("Caught SIGQUIT:  exiting gracefully"); $keep_going = 0; };

#
# loop principale
#
while ($keep_going) {

                my $newerr = 0;

		opendir(DIR, $DIR_INPUT) or $newerr=1;
		$log->error("Can't read directory  $!") if $newerr;

		while (my $name = readdir(DIR)) {
        		# Use a regular expression to ignore files beginning with a period
        		next if ($name =~ m/^\./);

                        $newerr = 0;


                        my $filename="$LOCAL_PATH$name";
			copy("$DIR_INPUT$name", $filename);
			
			my $type_account="";
			my $mobile_number="";
                        ($type_account,$mobile_number)=getMobileNumber($filename);

			$log->warning($type_account);
            if ($type_account ne "") {
                my $passwd = updateAccount($type_account,$mobile_number);
                my $sms_text = "";
				if ($type_account eq "100"){
					$sms_text = $template_password;
				} else {
					if ($passwd eq ""){
						$sms_text = $template_service;
					} else {
						$sms_text = $template_text;
					}
				}

                                $sms_text =~ s/<number>/$mobile_number/;
                                $sms_text =~ s/<password>/$passwd/;

                                my $fileout=writeSMSText($sms_text, "$LOCAL_PATH$mobile_number", $mobile_number);
				copy($fileout, "$DIR_OUTPUT$mobile_number");

                                $log->warning("Processed new account type $type_account number $mobile_number.");
                        }

                        unlink $filename;
			unlink "$DIR_INPUT$name"; 


        };
	
	closedir (DIR);

                sleep($polling_second);

}

#

# Mark a clean exit in the log
#
$log->warning("Stopping Processing:  ".time());


sub trim
{
        my $string = shift;
        $string =~ s/^\s+//;
        $string =~ s/\s+$//;
        return $string;
}

sub randomPassword {
        my $length_of_randomstring=shift;# the length of
                         # the random string to generate

        my @chars=('0'..'9');
        my $random_string;
        foreach (1..$length_of_randomstring)
        {
                # rand @chars will generate a random
                # number between 0 and scalar @chars
                $random_string.=$chars[rand @chars];
        }
        return $random_string;

}

sub getMobileNumber {
        my $filename=shift;
        my $newerr=0;
        my $text="";
        my $flag=0;
        my $type="";
        my $type_found="";
	my $max_time="";
        my @rvalue;
        
		$rvalue[0]="";
        $rvalue[1]="";
        
        open (FILE,$filename) or $newerr=1;
                $log->error("Unable to open $filename") if $newerr;
                return "" if $newerr;
        my $number="";
        while (my $text = <FILE>) {
                chomp($text);
                
		($type, $max_time) = checkTextSMS(trim($text));
		$MaxDailySession=$max_time;
                if ($type ne "") {
                		$type_found=$type;
                        $flag=1;
                }

                my ($key, $value) = split(":", $text);

                if ($key eq "From"){
                        $number = trim($value);
                        }
                }
        close (FILE);

        if ($flag==1){
		if (substr($number,0,2)=="39") {
                	$rvalue[0]=$type_found;
                	$rvalue[1]=$number;
		}
        } else {
        	$log->warning("Skipped sms without matched text.");
		}
        return @rvalue;

}

sub updateAccount {

		my $type_account = shift;
        my $mobile_number = shift;
        my $passwd = randomPassword(6);
        my $sha1_passwd = sha1_hex($passwd);


        #
        # Establish the connection which returns a DB handle
        #
        $log->notice("Preparing Connection $host $db $user $password!");

        my $dbh= DBI->connect("dbi:mysql:database=$db;host=$host",$user,$password)
        or die $log->error("Error Connecting MySQL DB!");

        $log->notice("Connection established!");
        #

	if ($type_account eq "100") {
	# type 100 - reset della password via sms con comando password
		$log->notice("Updating password {$mobile_number}");
		my $updateStatementP = "update radcheck set Value=? where Attribute='Password' and Username=?";
		my $sthP= $dbh->prepare($updateStatementP)
                or die $log->error("Error Prepared Statement!");

                $sthP->execute($sha1_passwd, $mobile_number)
                or die $log->error("Error executing update!");

                $sthP->finish();

	}
	else {

        	$log->notice("Inserting Account {$type_account} - {$mobile_number} - {$passwd}");
	

        	#
       		# Prepare the SQL statement
	        #		
	        my $sth= $dbh->prepare("select Username from radcheck where UserName=? and Attribute='Password' ")
	        or die $log->error("Error Prepared Statement!");

	        #
	        # Send the statement to the server
	        #		
	        $sth->execute($mobile_number)
	        or die $log->error("Error Executing Select!");

	        my $numRows = $sth->rows;

	        $sth->finish();

	        my $updateStatement="";
	        my $updateStatement1="";
	        my $updateStatement2="";
		my $updateStatement3="";
	        if($numRows == 0) {
        	        $updateStatement = "INSERT INTO radcheck (Value, Attribute, Username) VALUES (?, 'Password', ?)";
                	$updateStatement1 = "INSERT INTO radcheck (Value, Attribute, Username, op) VALUES ('1', 'Simultaneous-Use', ?,':=')";
	                #$updateStatement2 = "INSERT INTO radcheck (Value, Attribute, Username, op) VALUES ('$MaxDailySession', 'Max-Daily-Session', ?, ':=')";

        	        my $sth= $dbh->prepare($updateStatement)
	                or die $log->error("Error Prepared Statement!");

        	        $sth->execute($sha1_passwd, $mobile_number)
	                or die $log->error("Error executing update!");

	                $sth->finish();
	
        	        my $sth1= $dbh->prepare($updateStatement1)
                	or die $log->error("Error Prepared Statement!");

	                $sth1->execute($mobile_number)
        	        or die $log->error("Error executing update!");

                	$sth1->finish();

                        #my $sth2= $dbh->prepare($updateStatement2)
                        #or die $log->error("Error Prepared Statement!");

                	#$sth2->execute($mobile_number)
	                #or die $log->error("Error executing update!");

        	        #$sth2->finish();

			$updateStatement3 = "INSERT INTO radusertype (UserName, Valid, TypeAccount, CreationDate) VALUES (?, 1, ?, now())";
		
			my $sth3 = $dbh->prepare($updateStatement3)
			or die $log->error("Error Prepared Statement!");
 	
			$sth3->execute($mobile_number, $type_account)
			or die $log->error("Error executing update!");

			$sth3->finish();  

        	} else {
                	$updateStatement = "update radcheck set Value = ? where Username=? and Attribute='Password'";
			$updateStatement1 = "insert into radusertype (UserName, Valid, TypeAccount, CreationDate) VALUES (?, 1, ?, now()) ON DUPLICATE KEY UPDATE Valid=1, CreationDate = now()";

        	        my $sth= $dbh->prepare($updateStatement)
                	or die $log->error("Error Prepared Statement!");

	                $sth->execute($sha1_passwd, $mobile_number)
        	        or die $log->error("Error executing update!");

                	$sth->finish();

	                my $sth1= $dbh->prepare($updateStatement1)
        	        or die $log->error("Error Prepared Statement!");

                	$sth1->execute($mobile_number, $type_account)
	                or die $log->error("Error executing update!");

        	        $sth1->finish();

        	}
	}


        #

        #
        # Close the connection
        #
        $dbh->disconnect
        or die $log->error("Error Closing Connection!");

        $log->notice("Account Updated!");

        return $passwd;

}

sub writeSMSText {
        my $text = shift;
        my $filename = shift;
        my $number = shift;

        my $sms_message = "To: $number\n\n$text";

        open (MYFILE, '>', $filename);
        print MYFILE $sms_message;
        close (MYFILE);

        return $filename;

}

sub checkTextSMS {
	my $text = shift;
	$log->notice("Search for $text...");
	my $type = "";
	my @rvalue;
        $rvalue[0]="";
        $rvalue[1]="";

	my $sth = $dbh_csv->prepare("SELECT * FROM type_account_table WHERE text_sms=LOWER(?)");
    	$sth->execute($text);
    while (my $row = $sth->fetchrow_hashref) {
    	$log->notice($row->{'text_sms'});
    	$log->notice($row->{'type_account'});
        $rvalue[0] = $row->{'type_account'};
	$rvalue[1] = $row->{'max_time'};
    }
    $sth->finish();
	return @rvalue; 
}
