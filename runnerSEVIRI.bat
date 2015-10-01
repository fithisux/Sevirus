/*
args[0] the data folder
args[1] the csv of places
args[2] the output folder
args[3] static files
*/


set args_0=c:/downloads/seviri
set args_1=C:/downloads/places.csv
set args_2=C:/downloads/output
set args_3=C:/downloads/Static_HDF5_Files


java -Xmx2048m -jar seviritool.jar %args_0% %args_1% %args_2% %args_3%