# System Administration Guide

This document has everyting related to having the system up and running.

## Installation

Installation is covered in the [Installation Guide][1] document.

## Notify Me Configuration

`Notify Me` uses for configuration the file `/etc/notify-me.conf`

```bash
port=5000
host="localhost"
user="user"
password="password"
tts-command="/opt/notify-me/text2mp3.sh"
```

* `port` is the port number for the web server to use
* `host` is the database host name or ip address
* `user` is the database user
* `password` is the database password
* `tts-command` is the file doing the tts rendering to wave

It's assumed the tts executable file will output the rendered file name so you may need to wrap whatever you're using in a shell script. For a specific example you can look at the [Google TTS][2] shell script which uses the Google TTS online service.

## Asterisk Configuration

Asterisk configuration requires three steps.

### 1. Enable Manager API Access

Two steps are needed here, first modify the file `/etc/asterisk/manager.conf` enabling manager connections

```bash
[general]
enabled = yes
port = 5038
bindaddr = 0.0.0.0
```

The create a configuration file for your user, let's say your user is `pepe`, create the file

`/etc/asterisk/manager.d/pepe.conf`

With the following content, `secret` is the password, please change it.

```
[pepe]
secret=1234
read=all
write=all
writetimeout=1000
```

### 2. Create Dialing Context

After that you will need to create a context in `/etc/asterisk/extensions.conf` or any of the included files.

```bash
[notify-me-context]

exten => 1000,1, NOOP(${MESSAGE})
exten => 1000,2, AGI(render-wave.sh,"${MESSAGE}")
exten => 1000,3, Playback(beep);
```

This context will be used by the dialing trunk when a call is connected, priorities `1` and `3` are not really needed serving only for debugging purposes, you may use only the AGI invocation if you want.

### 3 Copy TTS AGI scripts

As you saw in the previous step, an AGI script is used in order to render the TTS text.

The text to be rendered is placed by the application in the `${MESSAGE}` variable so you may use any technique that suites your need in order to have the text be spoken using TTS.

If you have no particular preference you can take both the scripts available at [the google tts shell][2] and place them with execute permissions in the directory:

`/var/lib/asterisk/agi-bin/`

**Note: if you're using the google-tts script above, you will need a `sox` version installed that can read mp3 files.**

### 4. Setup Trunk

You can find the trunk configuration on the left side menu of the `Notify Me` application.

![Trunk Configuration](images/trunk.png?raw=true)

The fields to be configured are:

* `Name:` a unique identifier for this trunk
* `Technology:` the protocol used by the outgoing trunk in the pbx (SIP/IAX/etc)
* `Peer:` the peer name of the trunk, if you're dialing `SIP/crossphone/number` enter `crossphone` here
* `Context:` the context to place the call when connected, `notify-me-context` in the example above
* `Priority:` the priority to place the call when connected, `1` in the example above
* `Extension:` the extension to place the call when connected, `1000` in the example above
* `Caller Id:` the caller id to display to the contact when placing the call
* `Prefix:` prefix to add to the dialed number when placing the call on the line, can be empty
* `Capacity:` how many simultaneous calls can be placed using this trunk
* `Host:` the asterisk host name or ip address
* `User:` the asterisk manager user, `pepe` in the example above
* `Password:` the asterisk manager password, `1234` in the example above

Then you just select the trunk in the notification screen.

## SMS Configuration

There isn't much to be configured right now regarding sms dispatching, only the Ancel driver is available.

## Starting and Stopping

`Notify Me` is set up as a runit service when the package is installed, so the usual runit syntax applies:

* Start:
`sv start notify-me`
* Stop:
`sv stop notify-me`
* Force Stop(if hung):
`sv force-stop notify-me`

If the package `psmisc` is present you may issue the `pstree` command and see something like this:

```
init─┬─acpid
     ├─cron
     ├─daemon───mpt-statusd───sleep
     ├─6*[getty]
     ├─postgres───7*[postgres]
     ├─rsyslogd───3*[{rsyslogd}]
     ├─runsvdir───runsv─┬─java───33*[{java}]
     │                  └─svlogd
     ├─sshd───sshd───bash───pstree
     └─udevd───2*[udevd]
```

Where the `java` process child of `runsv` and `runsvdir` is the Notify Me application.

## Service Logs

Logs are created by runit service in `/var/log/notify-me/current`. Logs will be automatically rotated.

Usually to peek what's going on issue the following command:

`tail -f /var/log/notify-me/current`

Will give you the standard output of the program.

Please note as today log4j file is not modifiable, so you'll have to live with the default log settings.

[1]: installation-guide.md
[2]: https://github.com/guilespi/google-tts