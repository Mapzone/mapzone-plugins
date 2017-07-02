# Developing plugins for mapzone.io

## Introduction

mapzone.io is an open and extensible platform for processing and publishing of geospatial data. It is based on the open source [Polymap4](https://github.com/Polymap4) project, which in turn is based on Eclipse and the [Equinox](http://www.eclipse.org/equinox/) plugin framework. On top of this foundation mapzone.io provides an API that allows to develop plugins and run them on the platform. Plugins can extend mapzone.io in every conceivable way, like importing new data formats, extending the style editor or adding a new sharelet. Plugins can be privat to a user or organization or they can be publicly available. Public plugins can be used by all users of mapzone.io.

## Providing plugins

Everybody can develop plugins for mapzone.io and run them in all projects on mapzone.io he/she has admin permissions for. If you want to provide your plugins to other users of mapzone.io, you must agree to the [**Developer Agreement**](DeveloperAgreement.md). Basically the agreement says the following:

  * you own your product
  * you support your product
  * you choose fee, termination, ...
  * mapzone.io displays your product to users on your behalf
  * mapzone.io provides payment system and charges a transaction fee

A developer can provide **new versions** of the plugin at any time. The [Eclipse plugin versioning](https://wiki.eclipse.org/Version_Numbering) is used by the runtime system to ensure that the plugins are compatible to the main client version and other plugins. Project admins are informed about the update but the new version ist not installed automatically to any project.

## Using plugins

A user with admin privileges can choose a plugin to be used by an organization. If there is a fee for the plugin, then the admin is prompted to confirm that the monthly bill will be increased. Once activated a plugins can be used for **all projects** of an organization (or all personal projects of a user).

A plugin can be installed in one or more projects of the organization. The dashboard of the organization lists all plugins currently in use. A user can terminate using a plugin at any time.

## Guides and Tutorials

  * [Install and use Eclipse IDE](IDE.md)
