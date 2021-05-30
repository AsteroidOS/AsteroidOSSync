[![Contributors][contributors-shield]][contributors-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![Good First Issues][good-first-issue-shield]][good-first-issue-url]
[![GPL License][license-shield]][license-url]

AsteroidOS Sync
 ===============
<br />
<p align="center">
<a href = "https://asteroidos.org/">
<img alt="asteroidos logo" width="190" src="https://asteroidos.org/public/img/logo.png">
</a>
</div>

  <p align="center">
    Synchronize the time, notifications and more with your AsteroidOS smartwatch.
    <br />
    <br />
    <a href = "https://hosted.weblate.org/projects/asteroidos/sync/">
        <strong>Help us translate AsteroidOS Sync via Weblate </strong>
    </a>
    <br />
    <br />
        <a href="https://asteroidos.org/wiki/documentation/"><strong>Explore the docs </strong></a>
    <br />
    <a href="https://github.com/AsteroidOS/AsteroidOSSync/issues">Report Bug</a>
    .
    <a href="https://github.com/AsteroidOS/AsteroidOSSync/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement">Request Feature</a>
    <br />
    <a href="https://f-droid.org/packages/org.asteroidos.sync/" target="_blank">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="90"/></a>
   </p>
</p>

# Table of Contents 
* [About The Project](#about-the-project) 
* [Getting Started](#getting-started)
  * [Development Instructions](#development-instructions)
  * [Architecture of AsteroidOSSync](#architecture-of-asteroidossync) 
* [Roadmap](#roadmap)
* [Contributing](#contributing)
  * [Matrix](#matrix)

# About The Project 

AsteroidOS is an open-source operating system for smartwatches.

### Freedom & Privacy

We believe that when it comes to wearable devices, users should have full control over their machines and data. AsteroidOS has been created from the ground-up with modularity and freedom in mind. For this reason, it is a free and open-source project.

### Ready for your wrist

AsteroidOS unleashes the potential of your watch with up to 48 hours of autonomy and a set of apps including everything you need on a smartwatch: an agenda, an alarm clock, a calculator, a music controller, settings, a stopwatch, a timer and a weather forecast app.

### Proven building blocks

AsteroidOS is built upon a rock-solid base system. Qt and QML are used for fast and easy app development. OpenEmbedded provides a full GNU/Linux distribution and libhybris allows easy porting to most Android and Wear OS watches.

### Community-friendly

Since its creation in 2015, AsteroidOS has been led by Florent Revest, but is open towards external contribution and collaboration. The project is still in active development and if you want to help, make sure to check the Get Involved page.

# Getting Started 

AsteroidOSSync can be downloaded from f-droid [here](https://f-droid.org/packages/org.asteroidos.sync/) 

If you don't have a device running AsteroidOS, instructions for installation can be found [here](https://asteroidos.org/install/) 

### Development Instructions 

To get started with a developing for AsteroidOSSync, fork and clone the project, and import into any Android IDE. 

### Architecture of AsteroidOSSync

The [MainActivity](./app/src/main/java/org/asteroidos/sync/MainActivity.java) manages the UI fragments based on the current `IAsteroidDevice.ConnectionState` and starts a backend that is responsible for communication with the watch.

The Bluetooth backend is the [SynchronizationService](./app/src/main/java/org/asteroidos/sync/services/SynchronizationService.java). It is  responsible for talking to the watch via the [AsteroidBleManager](./app/src/main/java/org/asteroidos/sync/asteroid/AsteroidBleManager.java) and loading/unloading a service module that can talk to the watch.

A service, that implements [IConnectivityService](./app/src/main/java/org/asteroidos/sync/connectivity/IConnectivityService.java) (e.g. the [NotificationService](./app/src/main/java/org/asteroidos/sync/connectivity/NotificationService.java)), is a module that can send and receive data from a watch via a backend that implements the [IAsteroidDevice](./app/src/main/java/org/asteroidos/sync/asteroid/IAsteroidDevice.java) interface.

# Roadmap

See the [open issues](https://github.com/AsteroidOS/AsteroidOSSync/issues) for a list of proposed features and known issues 

# Contributing

AsteroidOS is driven by a vibrant community. We would love your help!

### Matrix

General discussions around AsteroidOS happen on the [#asteroid:matrix.org](https://matrix.to/#/#asteroid:matrix.org) channel on Matrix. Logs are available [here](https://log.asteroidos.org/).


[contributors-shield]: https://img.shields.io/github/contributors/AsteroidOS/AsteroidOSSync.svg?style=flat-square
[contributors-url]: https://github.com/AsteroidOS/AsteroidOSSync/graphs/contributors

[forks-shield]: https://img.shields.io/github/forks/AsteroidOS/AsteroidOSSync.svg?style=flat-square
[forks-url]: https://github.com/microsoft/AsteroidOS/AsteroidOSSync/members

[stars-shield]: https://img.shields.io/github/stars/AsteroidOS/AsteroidOSSync.svg?style=flat-square
[stars-url]: https://github.com/AsteroidOS/AsteroidOSSync/stargazers

[issues-shield]: https://img.shields.io/github/issues/AsteroidOS/AsteroidOSSync.svg?style=flat-square
[issues-url]: https://github.com/AsteroidOS/AsteroidOSSync/issues

[good-first-issue-shield]: https://img.shields.io/github/issues/AsteroidOS/AsteroidOSSync/good%20first%20issue?style=flat-square
[good-first-issue-url]: https://github.com/AsteroidOS/AsteroidOSSync/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22

[license-shield]: https://img.shields.io/github/license/AsteroidOS/AsteroidOSSync.svg?style=flat-square
[license-url]: https://github.com/AsteroidOS/AsteroidOSSync/blob/master/LICENSE
