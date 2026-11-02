
from onvif import ONVIFCamera

IP = ""
PORT = 8080
USER = ""
PASSWORD = ""

camera = ONVIFCamera(IP, PORT, USER, PASSWORD)

media = camera.create_media_service()

profiles = media.GetProfiles()

for profile in profiles:
    request = media.create_type("GetStreamUri")

    request.ProfileToken = profile.token
    request.StreamSetup = {
        "Stream": "RTP-Unicast",
        "Transport": {"Protocol": "RTSP"}
    }

    response = media.GetStreamUri(request)

    print(f"Perfil: {profile.Name}")
    print(f"URL RTSP: {response.Uri}")
