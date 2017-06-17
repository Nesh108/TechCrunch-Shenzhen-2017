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


$sql = "UPDATE `Team` SET `SCORE` = '0' WHERE `Team`.`ID` !=  -1" ;


if ($conn->query($sql) === TRUE) {
    echo "Record updated successfully";
} else {
    echo "Error updating record: " . $conn->error;
}

$conn->close();
?>