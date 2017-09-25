# Install and use Eclipse IDE

The [Eclipse IDE](http://eclipse.org) is used to develop plugins for mapzone.io. We provide an Eclipse extension that supports:

  * Creating new plugin projects connected to a mapzone.io project
  * Showing special properties of the plugin project
  * Exporting plugin to mapzone.io

## Install Eclipse IDE

  1. Install a [Java/PDE/RAP enabled version of Eclipse](http://www.eclipse.org/downloads/packages/eclipse-rcp-and-rap-developers/oxygenr).
  2. Install mapzone.io extension from **update site: http://build.mapzone.io/updatesite/**

**Beware**: The extension is not shown in any category. This is an open issue. **Please uncheck *Group items by category*!**

## Create a new project

A plugin development project for mapzone.io is just like any other Eclipse plugin project, except that is **connected** to a project on your mapzone.io account. When you create a new plugin development project then a **new target platform** is generated from the plugins of the mapzone.io project!

### Steps by step

  1. (Optional) Create a new workspace (for the new target platform)
  2. File -> New -> Other... -> mapzone.io/Plug-In Project
  3. Specify your mapzone.io account name and password
  4. Specify general project settings
  5. Finish

You may handle the code of the new plugin development project just like any other Eclipse plugin project, especially you can use your favourite version control system.

## Developing

During development you can **launch** the entire Mapzone (P4) client, including your new plugin, on your local computer. The automatically installed target platform contains anything you need to do so.

## Publish the new Plugin

Publishing a plugin means to make an entry in the global catalog of plugins so that other users can find and use it. You can publish your plugin whenever you think it is ready for prime time. In order to update the name or description and/or the plugin itself you can re-publish at any time.  

### Steps by step

  1. Right click project -> mapzone.io -> Publish Plugin...
  2. Login to your mapzone.io account
  4. Specify Plugin name and description for the global catalog of all Plugins
  5. Optionally check "Export new version of the plugin"
  6. Finish
