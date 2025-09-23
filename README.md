# Sevirus 3.0

## Description

A cross-platform tool for reprojection of Seviri and pixel extraction for Seviri and ASCAT products.


## Credits

It is built on:

* [OpenJFX](https://openjfx.io/)
* [Unidata NetCDF-Java](https://github.com/Unidata/netcdf-java)
* [GeoTools](https://www.geotools.org/)

and reuses (through separate well defined packages) :

* Geodesy

```
/* Geodesy by Mike Gavaghan
 *
 * http://www.gavaghan.org/blog/free-source-code/geodesy-library-vincentys-formula/
 *
 * This code may be freely used and modified on any personal or professional
 * project.  It comes with no warranty.
 *
 * BitCoin tips graciously accepted at 1FB63FYQMy7hpC2ANVhZ5mSgAZEtY1aVLf
 */
```

* FxDialogs 

Firstly proposed in a [StackOverflow Question](https://stackoverflow.com/questions/26341152/controlsfx-dialogs-deprecated-for-what)


* Retention File chooser (customized)

Firstly proposed in a [StackOverflow Question](https://stackoverflow.com/questions/36920131/can-a-javafx-filechooser-remember-the-last-directory-it-opened)



## Use

You need Maven. Run `mvn clean package` and just run the jar in the shade folder.

It is deprecated to download releases from [here](https://sourceforge.net/projects/seviri-tool/)
