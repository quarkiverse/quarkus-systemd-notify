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
    <version>1.0.0</version>
</dependency>
```

and configure the service unit file as follows:

```
...

[Service]
Type=notify
NotifyAccess=all

...
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
