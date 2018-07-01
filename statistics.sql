-- --------------------------------------------------------
-- Host:                         192.168.1.252
-- Server versie:                10.1.33-MariaDB-1~jessie - mariadb.org binary distribution
-- Server OS:                    debian-linux-gnu
-- HeidiSQL Versie:              9.5.0.5196
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


-- Databasestructuur van icrafted_statistics wordt geschreven
DROP DATABASE IF EXISTS `icrafted_statistics`;
CREATE DATABASE IF NOT EXISTS `icrafted_statistics` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `icrafted_statistics`;

-- Structuur van  tabel icrafted_statistics.player wordt geschreven
DROP TABLE IF EXISTS `player`;
CREATE TABLE IF NOT EXISTS `player` (
  `id` varchar(64) NOT NULL,
  `serverid` int(10) unsigned DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `isonline` tinyint(3) unsigned NOT NULL DEFAULT '0',
  UNIQUE KEY `id` (`id`),
  KEY `serverid` (`serverid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporteren was gedeselecteerd
-- Structuur van  tabel icrafted_statistics.server wordt geschreven
DROP TABLE IF EXISTS `server`;
CREATE TABLE IF NOT EXISTS `server` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `identifier` varchar(128) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- Data exporteren was gedeselecteerd
-- Structuur van  tabel icrafted_statistics.statistic_player wordt geschreven
DROP TABLE IF EXISTS `statistic_player`;
CREATE TABLE IF NOT EXISTS `statistic_player` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `serverid` int(11) DEFAULT NULL,
  `playerid` varchar(64) NOT NULL,
  `playtime` bigint(20) DEFAULT NULL COMMENT 'MS Time Connected',
  `deads` int(3) DEFAULT NULL,
  `damagetaken` double(10,1) DEFAULT NULL,
  `damagedealt` double(10,1) DEFAULT NULL,
  `attacklog` longtext,
  PRIMARY KEY (`id`),
  KEY `serverid` (`serverid`),
  KEY `playerid` (`playerid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- Data exporteren was gedeselecteerd
-- Structuur van  tabel icrafted_statistics.statistic_server wordt geschreven
DROP TABLE IF EXISTS `statistic_server`;
CREATE TABLE IF NOT EXISTS `statistic_server` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `serverid` int(10) unsigned DEFAULT NULL,
  `tps` double(4,2) NOT NULL,
  `onlineplayers` int(4) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `serverid` (`serverid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- Data exporteren was gedeselecteerd
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
