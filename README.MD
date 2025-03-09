![](https://i.imgur.com/LPh53qQ.png)

# OpenLR API

## Overview
This API provides OpenLR decoding capabilities, allowing users to convert binary OpenLR location references into human-readable formats. It supports decoding **LinearLocationReference** and **PointAlongLineLocationReference**, extracting relevant details such as reference points, functional road class, form of way, and calculated geographic coordinates.

## Dependencies
This API is built using:
- **[HERE Data SDK for Java/Scala](https://www.here.com/docs/bundle/here-workspace-developer-guide-java-scala/page/topics/how-to-use-sdk.html)** - Provides tools for location data processing and OpenLR decoding.
- **[HERE Flexible Polyline](https://github.com/heremaps/flexible-polyline)** - Used for encoding and decoding polylines efficiently.
- **[HERE Route Matching v8](https://developer.here.com/documentation/route-matching-api/8.7.0/dev_guide/index.html)** - Used for matching OpenLR decoded locations to real-world road networks.

### Prerequisites
Before using this API, you must:
1. Install the **HERE Data SDK**.
2. Obtain valid credentials from [HERE Platform](https://platform.here.com/).
3. Ensure access to **HERE Route Matching v8 API** for accurate road matching.

## API Endpoint
### `POST /decode`
This endpoint accepts a list of OpenLR binary-encoded strings (Base64 format) and returns a JSON representation of the decoded location reference.

### Request Example
```json
{
    "openlr_data": [
        "CC0BEAIpJN5Pc+9JxgAJBQQAAl0ACgQDACUAABz/6wAJBQQAAtwAAABABwwCAQA=",
        "CD0BEAA5ON4cEfFiqQAJBQQBA7cACgUEAelnAPEEAlEACQUEAQNIAHABz7YCswAJBQQBA8YACgUEAZ9VAAAA"
    ]
}
```

### Response Example
```json
{
    "decoded_results": [
        {
            "type": "LinearLocationReference",
            "firstReferencePoint": {
                "coordinate": {
                    "lat": -3.73625,
                    "lon": -38.51415
                },
                "lineProperties": {
                    "bearing": 142,
                    "bearingDegrees": 199.7,
                    "frc": 3,
                    "fow": 3,
                    "fowDescription": "SINGLE_CARRIAGEWAY"
                },
                "pathProperties": {
                    "lfrcnp": 3,
                    "dnp": 63,
                    "againstDrivingDirection": false
                }
            },
            "lastReferencePoint": {
                "coordinate": {
                    "lat": -3.73679,
                    "lon": -38.51433
                },
                "lineProperties": {
                    "bearing": 14,
                    "bearingDegrees": 19.7,
                    "frc": 3,
                    "fow": 3,
                    "fowDescription": "SINGLE_CARRIAGEWAY"
                }
            }
        }
    ]
}
```

## Features
- **OpenLR Decoding:** Supports decoding binary OpenLR data into structured JSON.
- **Geographic Coordinates:** Converts reference points into WGS84 latitude/longitude.
- **Road Classification:** Extracts information such as Functional Road Class (FRC) and Form of Way (FOW).
- **Route Matching:** Uses HERE Route Matching v8 API to integrate decoded locations with real-world road networks.

## Requirements
- Java 8+
- Jackson JSON Processing Library
- HERE Data SDK (Requires valid credentials from [HERE Platform](https://platform.here.com/))
- HERE Route Matching v8 API Access

## Installation & Setup
1. Clone the repository:
   ```sh
   git clone https://github.com/your-repo/openlr-api.git
   ```
2. Build the project:
   ```sh
   mvn clean install
   ```
3. Run the API server:
   ```sh
   java -jar target/openlr-api.jar
   ```

## License
This project is licensed under the MIT License.

