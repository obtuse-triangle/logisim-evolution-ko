<?php

# Configuration

define('QUARTUS', '/opt/altera/13.1/quartus/bin');
define('VAR_WWW', '/var/www/html/quartus');
define('RESULTS', 'results');
define('URIBASE', '/quartus/');


# Helpers

header("Content-Type: text/plain");
set_time_limit(240);

function str_endsWith( $haystack, $needle ) {
    $length = strlen( $needle );
    if( !$length ) {
        return true;
    }
    return substr( $haystack, -$length ) === $needle;
}

/**
 * Creates a random unique temporary directory, with specified parameters,
 * that does not already exist (like tempnam(), but for dirs).
 *
 * Created dir will begin with the specified prefix, followed by random
 * numbers.
 *
 * @link https://php.net/manual/en/function.tempnam.php
 *
 * @param string|null $dir Base directory under which to create temp dir.
 *     If null, the default system temp dir (sys_get_temp_dir()) will be
 *     used.
 * @param string $prefix String with which to prefix created dirs.
 * @param int $mode Octal file permission mask for the newly-created dir.
 *     Should begin with a 0.
 * @param int $maxAttempts Maximum attempts before giving up (to prevent
 *     endless loops).
 * @return string|bool Full path to newly-created dir, or false on failure.
 */
function tempdir($dir = null, $prefix = 'tmp_', $mode = 0700, $maxAttempts = 1000)
{
    /* Use the system temp dir by default. */
    if (is_null($dir))
    {
        $dir = sys_get_temp_dir();
    }

    /* Trim trailing slashes from $dir. */
    $dir = rtrim($dir, DIRECTORY_SEPARATOR);

    /* If we don't have permission to create a directory, fail, otherwise we will
     * be stuck in an endless loop.
     */
    if (!is_dir($dir) || !is_writable($dir))
    {
        return false;
    }

    /* Make sure characters in prefix are safe. */
    if (strpbrk($prefix, '\\/:*?"<>|') !== false)
    {
        return false;
    }

    /* Attempt to create a random directory until it works. Abort if we reach
     * $maxAttempts. Something screwy could be happening with the filesystem
     * and our loop could otherwise become endless.
     */
    $attempts = 0;
    do
    {
        $path = sprintf('%s%s%s%s', $dir, DIRECTORY_SEPARATOR, $prefix, mt_rand(100000, mt_getrandmax()));
    } while (
        !mkdir($path, $mode) &&
        $attempts++ < $maxAttempts
    );

    return $path;
}

function run_quartus_cmd($title, $cmd, $args) {
  
  echo "===============================================\n";
  echo $title . "\n";
  ob_flush(); flush();

  # $outname = tempnam("/tmp", "quartus");

  if (isset($_POST['use64bit']) && $_POST['use64bit'] == "1") {
    $cmd = $cmd . " --64bit";
  }
  $cmd = $cmd . " " . $args;

  echo "$cmd\n";
  ob_flush(); flush();
  $errcode = 0;
  if (passthru(QUARTUS . "/" . $cmd, $errcode) === FALSE) {
    echo "error: sorry, something went horribly wrong.\n";
    ob_flush(); flush();
  } else if (isset($errcode) && $errcode != 0) {
    echo "error: sorry, command failed (errcode: $errcode).\n";
    ob_flush(); flush();
  } else {
    return "success";
  }

  # $pid = exec(QUARTUS . "/$cmd < /dev/null > $outname 2>&1 & echo $!");
  # $pid = filter_var($pid, FILTER_VALIDATE_INT);

  # if (! isset($pid) || ! is_int($pid) || $pid < 5 ) {
  #   echo "error: sorry, something went horribly wrong (pid = $pid).\n";
  #   ob_flush(); flush();
  # } else {
  #   echo "...";
  #   ob_flush(); flush();
  #   $i = 0;
  #   while ($i < 235*4) {
  #     if (! file_exists("/proc/$pid")) { break; }
  #     echo ".";
  #     ob_flush(); flush();
  #     usleep(250000);
  #     $i = $i + 1;
  #   }
  #   if (file_exists("/proc/$pid")) {
  #     echo "timeout\nerror: quartus is taking too long, giving up!\n";
  #     ob_flush(); flush();
  #     posix_kill($pid, 9);
  #     unlink($outname);
  #   } else {
  #     echo "done, results to follow.\n";
  #     ob_flush(); flush();
  #     $unsafe_contents = file_get_contents($outname);
  #     unlink($outname);
  #     return $unsafe_contents;
  #   }
  # }
}

function exit_cleanup($qdir, $msg) {
  if (!is_null($qdir)) {
    # if (is_dir($qdir)) rmdir($qdir);
    # else unlink($qdir);
  }
  exit("error: $msg\n");
}

if (isset($_POST['operation']) && $_POST['operation'] == "list-cables") {
  run_quartus_cmd("Scanning for Attached Devices", "quartus_pgm", "--list");

} else if (isset($_POST['operation']) && $_POST['operation'] == "program") {
  #echo "0 in program...";
  #ob_flush(); flush();
  $uploadfile = $_FILES["bitfile"]["tmp_name"];
  #echo "a in program...";
  #ob_flush(); flush();
  $bitfile = tempnam(sys_get_temp_dir(), "bitfile_");
  #echo "b in program...";
  #ob_flush(); flush();
  if (str_endsWith($_FILES["bitfile"]["name"], ".sof")) {
  #echo "1 in program...";
  #ob_flush(); flush();
    rename($uploadfile, $bitfile .= '.sof');
  } else if (str_endsWith($_FILES["bitfile"]["name"], ".pof")) {
  #echo "2 in program...";
  #ob_flush(); flush();
    rename($uploadfile, $bitfile .= '.pof');
  } else {
  #echo "3 in program...";
  #ob_flush(); flush();
    exit("error: unrecognized file name\n");
  }

  $cablename = $_POST['cable'];

  if (isset($_POST['mode']) && ($_POST['mode'] == "as" || $_POST['mode'] == "jtag")) {
      $mode = $_POST['mode'];
    run_quartus_cmd("Downloading Bitstream", "quartus_pgm", "-c '$cablename' -m '$mode' -o 'P;$bitfile'")
      or exit_cleanup($bitfile, "bitstream programming failed");
    exit("success");
  } else {
    exit("error: unrecognized mode\n");
  }

} else if (isset($_POST['operation']) && $_POST['operation'] == "synthesize") {
  # echo "tmp_name " . $_FILES['zipfile']['tmp_name'] . "\n";
  # echo "exists " . file_exists($_FILES['zipfile']['tmp_name']) . "\n";
  # echo "uploaded " . is_uploaded_file($_FILES['zipfile']['tmp_name']) . "\n";
  # echo "request\n";
  # var_dump($_REQUEST);
  # echo "post\n";
  # var_dump($_POST);
  # echo "files\n";
  # var_dump($_FILES);
  # echo "end vars\n";
  if (!file_exists($_FILES['zipfile']['tmp_name']) || !is_uploaded_file($_FILES['zipfile']['tmp_name']))
    exit("error: must upload zip file\n");
  $ext = substr($_FILES["zipfile"]["name"], -4);
  if ($ext == "")
    exit("error: zipfile name is missing extension, should end in .zip\n");
  if ($ext != ".zip")
    exit("zipfile name should end in .zip");
  $uploadfile = $_FILES["zipfile"]["tmp_name"];
  echo "===============================================\n";
  echo "Preparing to invoke Quartus Synthesis toolchain\n";
  ob_flush(); flush();
  $qdir = tempdir("/tmp", "php_quartus_");
  $zip = new ZipArchive;
  $res = $zip->open($uploadfile);
  if ($res != TRUE) exit_cleanup($qdir, "could not unzip uploaded file");
  $res = $zip->extractTo($qdir);
  if ($res != TRUE) exit_cleanup($qdir, "could not unzip files");
  $zip->close();

  system("ls -lR $qdir");

  echo "sandbox: $qdir/sandbox\n";
  chdir("$qdir/sandbox") or exit_cleanup($qdir, "can't change into project sandbox directory");

  run_quartus_cmd("Creating Quartus Project", "quartus_sh", "-t ../scripts/AlteraDownload.tcl")
    or exit_cleanup($qdir, "project setup script failed");

  run_quartus_cmd("Optimizing for Minimal Area", "quartus_map", "LogisimToplevelShell --optimize=area")
    or exit_cleanup($qdir, "optimization failed");

  run_quartus_cmd("Synthesizing (may take a while)", "quartus_sh", "--flow compile LogisimToplevelShell")
    or exit_cleanup($qdir, "synthesis failed");

  echo "===============================================\n";
  echo "Summary of Fit Results\n";
  $unsafe_contents = file_get_contents("./LogisimToplevelShell.fit.summary");
  echo $unsafe_contents;
  echo "\n";

  if (isset($_POST['flashname'])) {
    $flashname = preg_replace('/[^a-zA-Z0-9]/', '_', $_POST['flashname']);
    run_quartus_cmd("Converting JTAG (.sof) to Flash (.pof) bitstream format",
      "quartus_cpf", "-c -d $flashname LogisimToplevelShell.sof LogisimToplevelShell.pof")
      or exit_cleanup($qdir, "bitstream conversion failed");
  }

  echo "===============================================\n";
  echo "Preparing Results\n";
  $resultname = RESULTS . "/bitstream-" . time() . ".zip";
  $resultpath = VAR_WWW . "/" . $resultname;
  chdir("..");
  system("zip -r $resultpath sandbox")
    or exit_cleanup($qdir, "failed to zip results");
  # system("echo chmod a+r $resultpath")
  #   or exit_cleanup($qdir, "failed to set permissions");

  echo "Success!\n";
  $resulturl = URIBASE . $resultname;
  echo "RESULT: $resulturl\n";

  #rmdir($qdir); # need to put this on a few-minute timer
}

?>
