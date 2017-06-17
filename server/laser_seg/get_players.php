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

$sql = "SELECT id, name, status, role, banner FROM Player";
$result = $conn->query($sql);

$players = array();

if ($result->num_rows > 0) {
    // output data of each row
    while($row = $result->fetch_assoc()) {

    	$players[$row["id"]] = array('name' => $row["name"], 'status' => $row["status"], 'role' => $row["role"], 'banner' => base64_encode($row["banner"]));
    	
    }
}

echo(json_encode($players));
$conn->close();
?>