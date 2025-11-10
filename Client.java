import java.io.*;
import java.net.*;
import java.util.*;


public class Client {
    // client.conf 파일에서 서버 접속 정보를 불러오는 메소드
    private static Properties loadConfig(String configFile) {
        Properties prop = new Properties();
        // 기본 접속 정보 설정 (HOST=localhost, PORT=9999)
        prop.setProperty("HOST", "localhost");
        prop.setProperty("PORT", "9999");
        
        try (FileInputStream input = new FileInputStream(configFile)) {
            // 파일이 존재하면 파일의 내용으로 접속 정보를 갱신
            prop.load(input);
        } catch (IOException e) {
            // 파일을 찾지 못하면 기본 접속 정보를 사용한다고 알림
            System.out.println("'" + configFile + "' not found. Using default settings (localhost:9999).");
        }
        // 설정 정보가 담긴 Properties 객체 반환
        return prop;
    }

    // 메인 메소드
    public static void main(String[] args) {
        // 변수 선언
        BufferedReader in = null;
        BufferedWriter out = null;
        Socket socket = null;
        // 사용자 입력을 받기 위한 Scanner 객체 생성
        Scanner scanner = new Scanner(System.in);

        // 설정 파일(client.conf) 불러오기
        Properties config = loadConfig("client.conf");
        // host 정보 불러오기
        String host = config.getProperty("HOST");
        // port 정보 불러오기
        int port = Integer.parseInt(config.getProperty("PORT"));

        try {
            // 서버에 연결 시도
            System.out.println("Attempting to connect to " + host + ":" + port + "...");
            socket = new Socket(host, port);
            // 서버에 연결 성공 시 메시지 출력
            System.out.println("Connected to server.");
            // 서버로부터 데이터를 받기 위한 입력 스트림 생성
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // 서버로 데이터를 보내기 위한 출력 스트림 생성
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
            // 서버와 계속 통신하기 위한 무한 루프
            while (true) {
                // 사용자로부터 계산식 입력받기
                System.out.print("Enter expression (e.g., ADD 1 2, bye to exit): ");
                String outputMessage = scanner.nextLine();
                
                // 입력받은 메시지를 서버로 전송
                out.write(outputMessage + "\n");
                out.flush();
                
                // 사용자가 "bye"를 입력하면 통신 종료
                if (outputMessage.equalsIgnoreCase("bye")) {
                    break;
                }
                
                // 서버로부터 계산 결과를 받아옴
                String inputMessage = in.readLine();
                if (inputMessage == null) { // 서버 연결이 끊겼을 경우
                    System.out.println("Server disconnected.");
                    break;
                }
                // 받은 결과를 화면에 출력
                System.out.println("Server response: " + inputMessage);
            }
        } catch (ConnectException e) { // 서버 연결이 거부되었을 때 예외 처리
            System.out.println("Connection refused. Is the server running at " + host + ":" + port + "?");
        } catch (IOException e) { // 그 외 입출력 예외 처리
            System.out.println("Error: " + e.getMessage());
        } finally {
            try {
                // 프로그램 종료 전 사용한 모든 리소스를 닫음
                scanner.close();
                if (socket != null) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}