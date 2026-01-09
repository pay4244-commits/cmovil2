<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

include_once '../db_connect.php';

$data = json_decode(file_get_contents("php://input"));

if(
    !empty($data->device_id) &&
    isset($data->battery_level)
){
    try {
        $query = "INSERT INTO device_data 
                 (device_id, phone_number, model, brand, os_version, battery_level, is_charging, latitude, longitude, altitude) 
                 VALUES 
                 (:device_id, :phone_number, :model, :brand, :os_version, :battery_level, :is_charging, :latitude, :longitude, :altitude)";

        $stmt = $conn->prepare($query);

        // Sanitize and bind
        $device_id = htmlspecialchars(strip_tags($data->device_id));
        $phone_number = isset($data->phone_number) ? htmlspecialchars(strip_tags($data->phone_number)) : null;
        $model = htmlspecialchars(strip_tags($data->model));
        $brand = htmlspecialchars(strip_tags($data->brand));
        $os_version = htmlspecialchars(strip_tags($data->os_version));
        $battery_level = intval($data->battery_level);
        $is_charging = $data->is_charging ? 1 : 0;
        $latitude = isset($data->latitude) ? $data->latitude : null;
        $longitude = isset($data->longitude) ? $data->longitude : null;
        $altitude = isset($data->altitude) ? $data->altitude : null;

        $stmt->bindParam(":device_id", $device_id);
        $stmt->bindParam(":phone_number", $phone_number);
        $stmt->bindParam(":model", $model);
        $stmt->bindParam(":brand", $brand);
        $stmt->bindParam(":os_version", $os_version);
        $stmt->bindParam(":battery_level", $battery_level);
        $stmt->bindParam(":is_charging", $is_charging);
        $stmt->bindParam(":latitude", $latitude);
        $stmt->bindParam(":longitude", $longitude);
        $stmt->bindParam(":altitude", $altitude);

        if($stmt->execute()){
            http_response_code(201);
            echo json_encode(array("message" => "Data recorded successfully."));
        } else {
            http_response_code(503);
            echo json_encode(array("message" => "Unable to record data."));
        }
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(array("message" => "Error: " . $e->getMessage()));
    }
} else {
    http_response_code(400);
    echo json_encode(array("message" => "Incomplete data. device_id and battery_level are required."));
}
?>