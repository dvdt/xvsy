# Load the data

ear,Month,DayofMonth,DayOfWeek,DepTime,CRSDepTime,ArrTime,CRSArrTime,UniqueCarrier,FlightNum,TailNum,ActualElapsedTime,CRSElapsedTime,AirTime,ArrDelay,DepDelay,Origin,Dest,Distance,TaxiIn,TaxiOut,Cancelled,CancellationCode,Diverted,CarrierDelay,WeatherDelay,NASDelay,SecurityDelay,LateAircraftDelay
2008,1,3,4,2003,1955,2211,2225,WN,335,N712SW,128,150,116,-14,8,IAD,TPA,810,4,8,0,,0,NA,NA,NA,NA,NA
2008,1,3,4,754,735,1002,1000,WN,3231,N772SW,128,145,113,2,19,IAD,TPA,810,5,10,0,,0,NA,NA,NA,NA,NA
2008,1,3,4,628,620,804,750,WN,448,N428WN,96,90,76,14,8,IND,BWI,515,3,17,0,,0,NA,NA,NA,NA,NA
2008,1,3,4,926,930,1054,1100,WN,1746,N612SW,88,90,78,-6,-4,IND,BWI,515,3,7,0,,0,NA,NA,NA,NA,NA
2008,1,3,4,1829,1755,1959,1925,WN,3920,N464WN,90,90,77,34,34,IND,BWI,515,3,10,0,,0,2,0,0,0,32
2008,1,3,4,1940,1915,2121,2110,WN,378,N726SW,101,115,87,11,25,IND,JAX,688,4,10,0,,0,NA,NA,NA,NA,NA
2008,1,3,4,1937,1830,2037,1940,WN,509,N763SW,240,250,230,57,67,IND,LAS,1591,3,7,0,,0,10,0,0,0,47

create sql temp table, then insert into actual table
2008,1,3,4,628,620,804,750,WN,448,N428WN,96,90,76,14,8,IND,BWI,515,3,17,0,,0,NA,NA,NA,NA,NA
```
CREATE TABLE flights
(
id serial primary key,
Year integer,
Month integer,
DayofMonth integer,
DayOfWeek integer,
DepTime integer,
CRSDepTime integer,
ArrTime integer,
CRSArrTime integer,
UniqueCarrier char(2),
FlightNum integer,
TailNum varchar(6),
ActualElapsedTime integer,
CRSElapsedTime integer,
AirTime integer,
ArrDelay integer,
DepDelay integer,
Origin char(3),
Dest char(3),
Distance integer,
TaxiIn integer,
TaxiOut integer,
Cancelled integer,
CancellationCode varchar(4),
Diverted integer,
CarrierDelay integer,
WeatherDelay integer,
NASDelay integer,
SecurityDelay integer,
LateAircraftDelay integer
);
CREATE TABLE tmp
(
Year integer ,
Month integer ,
DayofMonth integer ,
DayOfWeek integer ,
DepTime integer ,
CRSDepTime integer ,
ArrTime integer ,
CRSArrTime integer ,
UniqueCarrier char(2) ,
FlightNum integer ,
TailNum varchar(6) ,
ActualElapsedTime integer ,
CRSElapsedTime integer ,
AirTime integer ,
ArrDelay integer,
DepDelay integer,
Origin char(3),
Dest char(3),
Distance integer,
TaxiIn integer,
TaxiOut integer,
Cancelled integer,
CancellationCode varchar(4),
Diverted integer,
CarrierDelay integer,
WeatherDelay integer,
NASDelay integer,
SecurityDelay integer,
LateAircraftDelay integer
);

.separator ',' #sqlite
.import 2008.csv tmp


#postgres
COPY tmp FROM '/var/lib/postgresql/2008.csv' DELIMITER ',' CSV
HEADER NULL 'NA';

INSERT INTO flights
(
Year,
Month,
DayofMonth,
DayOfWeek,
DepTime,
CRSDepTime,
ArrTime,
CRSArrTime,
UniqueCarrier,
FlightNum,
TailNum,
ActualElapsedTime,
CRSElapsedTime,
AirTime,
ArrDelay,
DepDelay,
Origin,
Dest,
Distance,
TaxiIn,
TaxiOut,
Cancelled,
CancellationCode,
Diverted,
CarrierDelay,
WeatherDelay,
NASDelay,
SecurityDelay,
LateAircraftDelay
)
select
case when Year='NA' then null else Year end as Year,
case when Month='NA' then null else Month end as Month,
case when DayofMonth='NA' then null else DayofMonth end as DayofMonth,
case when DayOfWeek='NA' then null else DayOfWeek end as DayOfWeek,
case when DepTime='NA' then null else DepTime end as DepTime,
case when CRSDepTime='NA' then null else CRSDepTime end as CRSDepTime,
case when ArrTime='NA' then null else ArrTime end as ArrTime,
case when CRSArrTime='NA' then null else CRSArrTime end as CRSArrTime,
case when UniqueCarrier='NA' then null else UniqueCarrier end as UniqueCarrier,
case when FlightNum='NA' then null else FlightNum end as FlightNum,
case when TailNum='NA' then null else TailNum end as TailNum,
case when ActualElapsedTime='NA' then null else ActualElapsedTime end as ActualElapsedTime,
case when CRSElapsedTime='NA' then null else CRSElapsedTime end as CRSElapsedTime,
case when AirTime='NA' then null else AirTime end as AirTime,
case when ArrDelay='NA' then null else ArrDelay end as ArrDelay,
case when DepDelay='NA' then null else DepDelay end as DepDelay,
case when Origin='NA' then null else Origin end as Origin,
case when Dest='NA' then null else Dest end as Dest,
case when Distance='NA' then null else Distance end as Distance,
case when TaxiIn='NA' then null else TaxiIn end as TaxiIn,
case when TaxiOut='NA' then null else TaxiOut end as TaxiOut,
case when Cancelled='NA' then null else Cancelled end as Cancelled,
case when CancellationCode='NA' then null else CancellationCode end as CancellationCode,
case when Diverted='NA' then null else Diverted end as Diverted,
case when CarrierDelay='NA' then null else CarrierDelay end as CarrierDelay,
case when WeatherDelay='NA' then null else WeatherDelay end as WeatherDelay,
case when NASDelay='NA' then null else NASDelay end as NASDelay,
case when SecurityDelay='NA' then null else SecurityDelay end as SecurityDelay,
case when LateAircraftDelay='NA' then null else LateAircraftDelay end as LateAircraftDelay
from tmp;

DROP table tmp;
pg
---
INSERT INTO flights
(
Year,
Month,
DayofMonth,
DayOfWeek,
DepTime,
CRSDepTime,
ArrTime,
CRSArrTime,
UniqueCarrier,
FlightNum,
TailNum,
ActualElapsedTime,
CRSElapsedTime,
AirTime,
ArrDelay,
DepDelay,
Origin,
Dest,
Distance,
TaxiIn,
TaxiOut,
Cancelled,
CancellationCode,
Diverted,
CarrierDelay,
WeatherDelay,
NASDelay,
SecurityDelay,
LateAircraftDelay
)
select
Year,
Month,
DayofMonth,
DayOfWeek,
DepTime,
CRSDepTime,
ArrTime,
CRSArrTime,
UniqueCarrier,
FlightNum,
TailNum,
ActualElapsedTime,
CRSElapsedTime,
AirTime,
ArrDelay,
DepDelay,
Origin,
Dest,
Distance,
TaxiIn,
TaxiOut,
Cancelled,
CancellationCode,
Diverted,
CarrierDelay,
WeatherDelay,
NASDelay,
SecurityDelay,
LateAircraftDelay
from tmp;
```

create index on flights (dayofweek);
create index on flights (uniquecarrier);

bigquery
---

$  sed 's/NA//g' < 2008.csv > 2008-nulls.csv
$ head 2008-nulls.csv
Year ,Month ,DayofMonth ,DayOfWeek ,DepTime ,CRSDepTime ,ArrTime ,CRSArrTime ,UniqueCarrier ,FlightNum ,TailNum ,ActualElapsedTime ,CRSElapsedTime ,AirTime ,ArrDelay ,DepDelay ,Origin ,Dest ,Distance ,TaxiIn ,TaxiOut ,Cancelled ,CancellationCode ,Diverted ,CarrierDelay ,WeatherDelay ,SDelay ,SecurityDelay ,LateAircraftDelay
2008 ,1     ,3          ,4         ,2003    ,1955       ,2211    ,2225       ,WN            ,335       ,N712SW  ,128               ,150            ,116     ,-14      ,8        ,IAD    ,TPA  ,810      ,4      ,8       ,0         ,                 ,0        ,             ,             ,       ,              ,
2008 ,1     ,3          ,4         ,754     ,735        ,1002    ,1000       ,WN            ,3231      ,N772SW  ,128               ,145            ,113     ,2        ,19       ,IAD    ,TPA  ,810      ,5      ,10      ,0         ,                 ,0        ,             ,             ,       ,              ,
2008 ,1     ,3          ,4         ,628     ,620        ,804     ,750        ,WN            ,448       ,N428WN  ,96                ,90             ,76      ,14       ,8        ,IND    ,BWI  ,515      ,3      ,17      ,0         ,                 ,0        ,             ,             ,       ,              ,
2008 ,1     ,3          ,4         ,926     ,930        ,1054    ,1100       ,WN            ,1746      ,N612SW  ,88                ,90             ,78      ,-6       ,-4       ,IND    ,BWI  ,515      ,3      ,7       ,0         ,                 ,0        ,             ,             ,       ,              ,
2008 ,1     ,3          ,4         ,1829    ,1755       ,1959    ,1925       ,WN            ,3920      ,N464WN  ,90                ,90             ,77      ,34       ,34       ,IND    ,BWI  ,515      ,3      ,10      ,0         ,                 ,0        ,2            ,0            ,0      ,0             ,32
2008 ,1     ,3          ,4         ,1940    ,1915       ,2121    ,2110       ,WN            ,378       ,N726SW  ,101               ,115            ,87      ,11       ,25       ,IND    ,JAX  ,688      ,4      ,10      ,0         ,                 ,0        ,             ,             ,       ,              ,
2008 ,1     ,3          ,4         ,1937    ,1830       ,2037    ,1940       ,WN            ,509       ,N763SW  ,240               ,250            ,230     ,57       ,67       ,IND    ,LAS  ,1591     ,3      ,7       ,0         ,                 ,0        ,10           ,0            ,0      ,0             ,47
2008 ,1     ,3          ,4         ,1039    ,1040       ,1132    ,1150       ,WN            ,535       ,N428WN  ,233               ,250            ,219     ,-18      ,-1       ,IND    ,LAS  ,1591     ,7      ,7       ,0         ,                 ,0        ,             ,             ,       ,              ,
2008 ,1     ,3          ,4         ,617     ,615        ,652     ,650        ,WN            ,11        ,N689SW  ,95                ,95             ,70      ,2        ,2        ,IND    ,MCI  ,451      ,6      ,19      ,0         ,                 ,0        ,             ,             ,       ,              ,

Year:integer,Month:integer,DayofMonth:integer,DayOfWeek:integer,DepTime:integer,CRSDepTime:integer,ArrTime:integer,CRSArrTime:integer,UniqueCarrier:string,FlightNum:string,TailNum:string,ActualElapsedTime:integer,CRSElapsedTime:integer,AirTime:integer,ArrDelay:integer,DepDelay:integer,Origin:string,Dest:string,Distance:integer,TaxiIn:integer,TaxiOut:integer,Cancelled:integer,CancellationCode:string,Diverted:integer,CarrierDelay:integer,WeatherDelay:integer,NASDelay:integer,SecurityDelay:integer,LateAircraftDelay:integer

SELECT COUNT(month), "flights"."uniquecarrier" AS, "flights"."actualelapsedtime" AS "y", WIDTH_BUCKET("flights"."distance", ?, ?, ?) AS "x" FROM "flights" GROUP BY "y", "x", "color" ORDER BY "x" ASC, "color" ASC, "fill" ASC, "y" ASC



SELECT COUNT(month) AS fill, uniquecarrier AS color,
actualelapsedtime AS y, distance as x FROM [flights.2008] GROUP BY
y, x, color ORDER BY x ASC, color ASC, fill ASC, y ASC
