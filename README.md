# Aproveitamento de Disciplinas
## API
### Primeira Conexão
URL https://ifconnect-api.vercel.app
PAYLOAD /api
TOKEN i2KRCWgS1OHuivC8jDz1fFkuGFllHtqcEzkL7UEu6xc3ksWwkOw3FIn52yB4Wx63Se93ChoX0I8ezhmeFH04blhZdNZmxe0U0ne8WXZ6Z68DcRecZEPHqvrRarIZ__Bjps0xCzwncUo

- Python
``` 
import requests

token = "i2KRCWgS1OHuivC8jDz1fFkuGFllHtqcEzkL7UEu6xc3ksWwkOw3FIn52yB4Wx63Se93ChoX0I8ezhmeFH04blhZdNZmxe0U0ne8WXZ6Z68DcRecZEPHqvrRarIZ__Bjps0xCzwncUo"

response = requests.get("https://ifconnect-api.vercel.app/api", headers={"Authorization": f"Bearer {token}"})

print(response.status_code)
print(response.json())
```

- Java
``` 
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main {
    public static void main(String[] args) {
        String token = "i2KRCWgS1OHuivC8jDz1fFkuGFllHtqcEzkL7UEu6xc3ksWwkOw3FIn52yB4Wx63Se93ChoX0I8ezhmeFH04blhZdNZmxe0U0ne8WXZ6Z68DcRecZEPHqvrRarIZ__Bjps0xCzwncUo";

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ifconnect-api.vercel.app/api"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Status: " + response.statusCode());
            System.out.println("Resposta: " + response.body());

        } catch (Exception e) {
            System.out.println("Erro na requisição: " + e.getMessage());
        }
    }
}
```
