# Install and use Eclipse IDE

The [Eclipse IDE](http://eclipse.org) is used to develop plugins for mapzone.io. On top of this we provide an extension that supports:

  * creating new plugin projects connected to a mapzone.io project
  * show special properties of the plugin project
  * exporting plugin to mapzone.io

## Install Eclipse IDE

  1. install a [Java/PDE/RAP enabled version of Eclipse](http://www.eclipse.org/downloads/packages/eclipse-rcp-and-rap-developers/oxygenr).
  2. install mapzone.io extension from **update site: http://build.mapzone.io/updatesite/**

## Create a new project

A plugin development project for mapzone.io is just like any other Eclipse plugin project, except that is **connected** to a project on your mapzone.io account. When you create a new plugin development project then a **new target platform** is generated from the plugins of the mapzone.io project!

Steps to create a new plugin development project:

  1. (optional) create a new workspace (for the new target platform)
  2. File -> New -> Other... -> mapzone.io/Plug-In Project
  3. specify yourr mapzone.io account name and password
  4. specify general project settings
  5. Finish
