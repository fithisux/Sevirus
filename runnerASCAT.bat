/*
args[0] the data folder
args[1] the csv of places
args[2] the csv of output
args[3] product type FVC/LST/ET
args[4] static files
*/


set args_0=c:/downloads/ascat
set args_1=C:/downloads/places.csv
set args_2=C:/downloads/ascat_output.csv


java -Xmx2048m -cp seviritool.jar org.ntua.headless.RunAscatProcessor %args_0% %args_1% %args_2% 