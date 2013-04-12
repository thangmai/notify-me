# Installation Guide

This guide covers from the compile and build stage to the complete deploy of the sollution using [Pallet][1].

In order to be able to install the software or deploy a change, these are the stages to follow:

1. Compile and build the solution
2. Make the distributable package
3. Deploy the solution to selected nodes using Pallet.

Packages are only provided for `debian` distributions so far, that may change in the future.

## Compile and Build

You need to have [leiningen][2] installed project was tested using version `2.1.2`.

Switch to the project directory and issue the command `lein uberjar`, it will make a bundle of about 60mb with all the project dependencies.

That's all.

## Debian Packaging

All the scripts for debian packaging are located in the `packaging` directory of the solution.

Issue the command `./packaging/bin/paq.sh` in the project directory and it will create the `.deb` file.

Depending on the version you're building you may end up with a file like this:

`notify-me_0.4.0-0000_amd64.deb`

## Pallet

Pallet is used for

### Groups

There are two distinct groups in our project:

* Telephony centrals using Asterisk, named `asterisk`
* Application servers to install our app, named `notify-me`

Each group has its own set of packages, rules and dependencies.

### Dependencies

Please note `sox` is being installed in both the groups, but it not always works with that combination of packages and should be setup by hand.
Also `git` is being installed in both nodes but not really needed for operation.

#### Asterisk

* Sox and libsox-fmt-mp3

#### Notify Me

* Postgres SQL
* Runit
* Java 7, Sun
* Sox and libsox-fmt-mp3

### Commands

To setup only the group `notify-me` to the `service-name` service:

`lein pallet -P service-name up --groups notify-me --phases pre-req,install,configure`

To remotely create the initial database (so far this is not being done by pallet)

`lein trampoline run -m tasks.db rebuild`

Create the database initial data (administration user basically)

`lein trampoline run -m tasks.db seed`

[1]: https://github.com/pallet
[2]: https://github.com/technomancy/leiningen
