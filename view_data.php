<?php
include_once 'db_connect.php';

$query = "SELECT * FROM device_data ORDER BY timestamp DESC";
$stmt = $conn->prepare($query);
$stmt->execute();
$data = $stmt->fetchAll(PDO::FETCH_ASSOC);
?>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Datos de Dispositivos - DataCollector</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { padding: 20px; background-color: #f8f9fa; }
        .table-container { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        h1 { color: #343a40; margin-bottom: 20px; }
    </style>
</head>
<body>
    <div class="container-fluid">
        <div class="table-container">
            <h1>📱 Dispositivos Rastreados</h1>
            <div class="table-responsive">
                <table class="table table-striped table-hover">
                    <thead class="table-dark">
                        <tr>
                            <th>ID</th>
                            <th>Device ID</th>
                            <th>Modelo</th>
                            <th>Batería</th>
                            <th>Cargando</th>
                            <th>Ubicación (Lat, Lon)</th>
                            <th>Fecha/Hora</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if(count($data) > 0): ?>
                            <?php foreach($data as $row): ?>
                                <tr>
                                    <td><?php echo htmlspecialchars($row['id']); ?></td>
                                    <td><?php echo htmlspecialchars($row['device_id']); ?></td>
                                    <td><?php echo htmlspecialchars($row['brand'] . " " . $row['model']); ?></td>
                                    <td>
                                        <div class="progress">
                                            <div class="progress-bar <?php echo $row['battery_level'] < 20 ? 'bg-danger' : ($row['battery_level'] < 50 ? 'bg-warning' : 'bg-success'); ?>" 
                                                 role="progressbar" 
                                                 style="width: <?php echo $row['battery_level']; ?>%">
                                                <?php echo $row['battery_level']; ?>%
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <?php if($row['is_charging']): ?>
                                            <span class="badge bg-success">Sí</span>
                                        <?php else: ?>
                                            <span class="badge bg-secondary">No</span>
                                        <?php endif; ?>
                                    </td>
                                    <td>
                                        <?php 
                                            if($row['latitude'] && $row['longitude']) {
                                                echo "<a href='https://www.google.com/maps?q={$row['latitude']},{$row['longitude']}' target='_blank'>Ver Mapa</a>";
                                            } else {
                                                echo "N/A";
                                            }
                                        ?>
                                    </td>
                                    <td><?php echo htmlspecialchars($row['timestamp']); ?></td>
                                </tr>
                            <?php endforeach; ?>
                        <?php else: ?>
                            <tr>
                                <td colspan="7" class="text-center">No hay datos registrados aún.</td>
                            </tr>
                        <?php endif; ?>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
</html>
