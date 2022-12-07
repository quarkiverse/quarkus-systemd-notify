# Quarkus Systemd Notify Extension

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.systemd.notify/quarkus-systemd-notify?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/io.quarkiverse.systemd.notify/quarkus-systemd-notify)

## Intoduction

This extension is used to notify Linux service manager (systemd) about start-up completion and other service status changes.

## Usage

To use the extension, add the dependency to the target project:

```
<dependency>
    <groupId>io.quarkiverse.systemd.notify</groupId>
    <artifactId>quarkus-systemd-notify</artifactId>
    <version>0.1.0</version>
</dependency>
```

and configure the service unit file with the following minumum configurations:

```
...

[Service]
Type=notify
NotifyAccess=all

...
```

## Systemd Service Example

Assuming `quarkus-run.jar` is located at `/opt/quarkus-app/quarkus-run.jar`:

- Create a unit configuration file at `/etc/systemd/system/quarkus.service`:

```
[Unit]
Description=Quarkus Server
After=network.target
Wants=network.target

[Service]
Type=notify
NotifyAccess=all
ExecStart=/bin/java -jar /opt/quarkus-app/quarkus-run.jar
SuccessExitStatus=0 143

[Install]
WantedBy=multi-user.target
```
- Enable the service (this will make it to run at system start-up as well):

```
sudo systemctl enable quarkus
```

- Start/Stop/Restart the service:

```
sudo systemctl start quarkus
sudo systemctl stop quarkus
sudo systemctl restart quarkus
```

- check status of the service:

```
sudo systemctl status quarkus
```

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center"><a href="https://fouad.io"><img src="https://avatars.githubusercontent.com/u/1194488?v=4?s=100" width="100px;" alt="Fouad Almalki"/><br /><sub><b>Fouad Almalki</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-systemd-notify/commits?author=Eng-Fouad" title="Code">💻</a> <a href="#maintenance-Eng-Fouad" title="Maintenance">🚧</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
