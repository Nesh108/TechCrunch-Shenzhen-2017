<?php
$servername = "localhost";
$username = "root";
$password = "codemao123";
$dbname = "laser_seg";

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);
// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
} 

if(!isset($_GET["id"])) {
    die("ID not provided!");
}

if(!isset($_GET["score"])) {
    die("Score not provided!");
}
$sql = "UPDATE `Team` SET `SCORE` = '" . $_GET["score"] . "' WHERE `Team`.`ID` = " . $_GET["id"];


if ($conn->query($sql) === TRUE) {
    echo "Record updated successfully";
} else {
    echo "Error updating record: " . $conn->error;
}

$conn->close();
?>