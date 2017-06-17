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

$sql = "SELECT id, name, score FROM Team";
$result = $conn->query($sql);

$teams = array();

if ($result->num_rows > 0) {
    // output data of each row
    while($row = $result->fetch_assoc()) {

    	$teams[$row["id"]] = array('name' => $row["name"], 'score' => $row["score"]);
    	
    }
}

echo(json_encode($teams));
$conn->close();
?>