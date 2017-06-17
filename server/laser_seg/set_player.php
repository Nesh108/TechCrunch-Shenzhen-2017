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



$status = "ALIVE";
if($_GET['status'] === "0") {
    $status = "DEAD";
}

$role = $_GET['role'];
if(!isset($role)) {
    $sql = "UPDATE `Player` SET `STATUS` = '" . $status . "' WHERE `Player`.`ID` = " . $_GET["id"];
} else {
    $sql = "UPDATE `Player` SET `STATUS` = '" . $status . "', `ROLE` = '" . $role . "' WHERE `Player`.`ID` = " . $_GET["id"];
}

if ($conn->query($sql) === TRUE) {
    echo "Record updated successfully";
} else {
    echo "Error updating record: " . $conn->error;
}

$conn->close();
?>