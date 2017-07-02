# Install and use Eclipse IDE

The [Eclipse IDE](http://eclipse.org) is used to develop plugins for mapzone.io. We provide an Eclipse extension that supports:

  * creating new plugin projects connected to a mapzone.io project
  * show special properties of the plugin project
  * exporting plugin to mapzone.io

## Install Eclipse IDE

  1. install a [Java/PDE/RAP enabled version of Eclipse](http://www.eclipse.org/downloads/packages/eclipse-rcp-and-rap-developers/oxygenr).
  2. install mapzone.io extension from **update site: http://build.mapzone.io/updatesite/**

**Beware**: The extension is not shown in any category. This is an open issue. **Please uncheck *Group items by category*!**

## Create a new project

A plugin development project for mapzone.io is just like any other Eclipse plugin project, except that is **connected** to a project on your mapzone.io account. When you create a new plugin development project then a **new target platform** is generated from the plugins of the mapzone.io project!

### Steps to create a new plugin development project:

  1. (optional) create a new workspace (for the new target platform)
  2. File -> New -> Other... -> mapzone.io/Plug-In Project
  3. specify your mapzone.io account name and password
  4. specify general project settings
  5. Finish

You may handle the code of the new plugin development project just like any other Eclipse plugin project, especially you can use your favourite version control system.
