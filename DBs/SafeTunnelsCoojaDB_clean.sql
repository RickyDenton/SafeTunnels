CREATE DATABASE  IF NOT EXISTS `SafeTunnelsCoojaDB` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `SafeTunnelsCoojaDB`;
-- MySQL dump 10.13  Distrib 5.7.42, for Linux (x86_64)
--
-- Host: localhost    Database: SafeTunnelsCoojaDB
-- ------------------------------------------------------
-- Server version	5.7.42-0ubuntu0.18.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `actuators`
--

DROP TABLE IF EXISTS `actuators`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `actuators` (
  `actuatorID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `mac` varchar(75) NOT NULL,
  PRIMARY KEY (`actuatorID`),
  UNIQUE KEY `mac_UNIQUE` (`mac`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `actuators`
--

LOCK TABLES `actuators` WRITE;
/*!40000 ALTER TABLE `actuators` DISABLE KEYS */;
INSERT INTO `actuators` VALUES (1,'00:04:00:04:00:04:00:04'),(2,'00:05:00:05:00:05:00:05');
/*!40000 ALTER TABLE `actuators` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `actuatorsConnStateSeries`
--

DROP TABLE IF EXISTS `actuatorsConnStateSeries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `actuatorsConnStateSeries` (
  `actuatorID` int(10) unsigned NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `connState` tinyint(1) unsigned NOT NULL,
  PRIMARY KEY (`actuatorID`,`timestamp`),
  CONSTRAINT `fk_actuatorsConnStateSeries_1` FOREIGN KEY (`actuatorID`) REFERENCES `actuators` (`actuatorID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `actuatorsConnStateSeries`
--

LOCK TABLES `actuatorsConnStateSeries` WRITE;
/*!40000 ALTER TABLE `actuatorsConnStateSeries` DISABLE KEYS */;
/*!40000 ALTER TABLE `actuatorsConnStateSeries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `actuatorsFanSeries`
--

DROP TABLE IF EXISTS `actuatorsFanSeries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `actuatorsFanSeries` (
  `actuatorID` int(10) unsigned NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fanRelSpeed` tinyint(3) unsigned NOT NULL,
  PRIMARY KEY (`actuatorID`,`timestamp`),
  CONSTRAINT `fk_actuatorsFanSeries_1` FOREIGN KEY (`actuatorID`) REFERENCES `actuators` (`actuatorID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `actuatorsFanSeries`
--

LOCK TABLES `actuatorsFanSeries` WRITE;
/*!40000 ALTER TABLE `actuatorsFanSeries` DISABLE KEYS */;
/*!40000 ALTER TABLE `actuatorsFanSeries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `actuatorsLightSeries`
--

DROP TABLE IF EXISTS `actuatorsLightSeries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `actuatorsLightSeries` (
  `actuatorID` int(10) unsigned NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lightState` varchar(30) NOT NULL,
  PRIMARY KEY (`actuatorID`,`timestamp`),
  CONSTRAINT `fk_actuatorsLightSeries_1` FOREIGN KEY (`actuatorID`) REFERENCES `actuators` (`actuatorID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `actuatorsLightSeries`
--

LOCK TABLES `actuatorsLightSeries` WRITE;
/*!40000 ALTER TABLE `actuatorsLightSeries` DISABLE KEYS */;
/*!40000 ALTER TABLE `actuatorsLightSeries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sensors`
--

DROP TABLE IF EXISTS `sensors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sensors` (
  `sensorID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `mac` varchar(75) NOT NULL,
  PRIMARY KEY (`sensorID`),
  UNIQUE KEY `mac_UNIQUE` (`mac`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sensors`
--

LOCK TABLES `sensors` WRITE;
/*!40000 ALTER TABLE `sensors` DISABLE KEYS */;
INSERT INTO `sensors` VALUES (1,'00:02:00:02:00:02:00:02'),(2,'00:03:00:03:00:03:00:03');
/*!40000 ALTER TABLE `sensors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sensorsC02Series`
--

DROP TABLE IF EXISTS `sensorsC02Series`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sensorsC02Series` (
  `sensorID` int(10) unsigned NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `C02Density` int(11) NOT NULL,
  PRIMARY KEY (`sensorID`,`timestamp`),
  CONSTRAINT `fk_sensorsC02Series_1` FOREIGN KEY (`sensorID`) REFERENCES `sensors` (`sensorID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sensorsC02Series`
--

LOCK TABLES `sensorsC02Series` WRITE;
/*!40000 ALTER TABLE `sensorsC02Series` DISABLE KEYS */;
/*!40000 ALTER TABLE `sensorsC02Series` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sensorsConnStateSeries`
--

DROP TABLE IF EXISTS `sensorsConnStateSeries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sensorsConnStateSeries` (
  `sensorID` int(10) unsigned NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `connState` tinyint(1) unsigned NOT NULL,
  PRIMARY KEY (`sensorID`,`timestamp`),
  CONSTRAINT `fk_sensorsConnStateSeries_1` FOREIGN KEY (`sensorID`) REFERENCES `sensors` (`sensorID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sensorsConnStateSeries`
--

LOCK TABLES `sensorsConnStateSeries` WRITE;
/*!40000 ALTER TABLE `sensorsConnStateSeries` DISABLE KEYS */;
/*!40000 ALTER TABLE `sensorsConnStateSeries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sensorsTempSeries`
--

DROP TABLE IF EXISTS `sensorsTempSeries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sensorsTempSeries` (
  `sensorID` int(5) unsigned NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `temp` int(11) NOT NULL,
  PRIMARY KEY (`timestamp`,`sensorID`),
  KEY `fk_sensorsTempSeries_1_idx` (`sensorID`),
  CONSTRAINT `fk_sensorsTempSeries_1` FOREIGN KEY (`sensorID`) REFERENCES `sensors` (`sensorID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sensorsTempSeries`
--

LOCK TABLES `sensorsTempSeries` WRITE;
/*!40000 ALTER TABLE `sensorsTempSeries` DISABLE KEYS */;
/*!40000 ALTER TABLE `sensorsTempSeries` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2023-06-16  4:25:06
