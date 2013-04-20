# Notify Me

`Notify Me` is an application created for massive outgoing communication, using text messaging or call dispatching connected with an [Asterisk PBX][1]. Usually known as a `Voice Blaster` or `SMS Blaster`

![Notify Me Login](doc/images/login.png?raw=true)

## Purpose

In many cases, particularly emergency situations, it's needed to automatically dispatch a message to potentially thousands of recipients in a single click.

Main features are:

* Multi-tenant solution, allows for different offices operating on the same infraestructure.
* Contacts and groups of contacts can be created if pre-arranged notification groups are needed.
* TTS using any solution that provides a command line translator.
* Telephony integrates with minimum hassle using [Manager API][2].
* Progress charts and detail to see contact rates.

<br/>

![charts](doc/images/running_notification.png?raw=true)
<br/>

## Integration

In order for the solution to be able to dispatch anything some configuration is needed.
Please look at the [System Administration][3] guide for details.

### Asterisk

Integration with Asterisk PBX is done using [clj-asterisk][4] bindings for Manager API.

Main configuration points are:

* Enable Asterisk Manager connections in file `/etc/asterisk/manager.conf`
* Create an outgoing dialing context in `/etc/asterisk/extensions.conf`
* Create a trunk configuration in Notify-Me

After that the trunk may be used when creating a new notification.

### SMS

Now, SMS integration is done only with `Ancel SMS Empresa` using the [clj-ancel-sms][7] library.

Extension is not only desirable but possible, since each sms provider is treated as a plugin complying with a dispatching protocol.

## Docs

Documentation is a work in progress

* [System Administration Guide][3]
* [Installation Guide][5]
* [User Guide][6]

## TODOs

* i18n
* Use chiba's plugin for sms providers
* Integrate selenium tests for UI
* Pagination, grids and dispatchers

## License

Copyright © 2013 Guillermo Winkler

Distributed under the Eclipse Public License, the same as Clojure.

[1]: http://www.asterisk.org
[2]: http://www.voip-info.org/wiki/view/Asterisk+manager+API
[3]: doc/system-administration-guide.md
[4]: https://github.com/guilespi/clj-asterisk
[5]: doc/installation-guide.md
[6]: doc/user-guide.md
[7]: https://github.com/guilespi/clj-ancel-sms
