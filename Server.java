import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    // 클라이언트로부터 받은 계산식을 처리하는 메소드
    public static String calc(String exp){
        // 문자열을 공백 기준으로 나누기 위한 StringTokenizer 생성
        StringTokenizer st = new StringTokenizer(exp);
        
        // 입력된 인자의 개수가 3개가 아닌 경우 오류 처리
        if(st.countTokens() != 3){
            if(st.countTokens() < 3){
                System.out.println("Wrong Input: Not enough arguments");
                return "Wrong Input: Not enough arguments";
            }
            if(st.countTokens() > 3){
                System.out.println("Wrong Input: Too many arguments");
                return "Wrong Input: Too many arguments";
            }
        }

        String res = "";
        // 첫 번째 토큰을 연산자로 저장
        String opcode = st.nextToken();
        // 두 번째, 세 번째 토큰을 피연산자 문자열로 저장
        String op1initial = st.nextToken();
        String op2initial = st.nextToken();
        // 정수형 피연산자를 저장할 변수 선언
        int op1;
        int op2;

        // 피연산자 문자열을 정수로 변환 시도
        try{
            op1 = Integer.parseInt(op1initial);
            op2 = Integer.parseInt(op2initial);
        }catch(NumberFormatException e){ // 정수로 변환 실패 시 예외 처리
            System.out.println("Wrong Input: Must be an integer");
            return "Wrong Input: Must be an integer";
        }
        
        // 연산자(opcode)에 따라 계산 수행
        switch (opcode.toUpperCase()){
            case "ADD":
                res = Integer.toString(op1 + op2);
                System.out.println("ADD " + op1 + " " + op2 + " = " + res);
                break;
            case "MIN":
                res = Integer.toString(op1 - op2);
                System.out.println("MIN " + op1 + " " + op2 + " = " + res);
                break;
            case "MUL":
                res = Integer.toString(op1 * op2);
                System.out.println("MUL " + op1 + " " + op2 + " = " + res);
                break;
            case "DIV":
                if(op2 == 0){ // 0으로 나누는 경우 예외 처리
                    System.out.println("Wrong Input : Division by zero");
                    return "Wrong Input : Division by zero";
                }
                res = Integer.toString(op1 / op2);
                System.out.println("DIV " + op1 + " " + op2 + " = " + res);
                break;
            default: // 정해진 연산자가 아닐 경우 예외 처리
                res = "Wrong Input : Invalid opcode";
                System.out.println("Wrong Input : Invalid opcode");
                break;
        }
        // 계산 결과 반환
        return res;
    }

    // server.conf 파일에서 포트 번호를 읽어오는 메소드
    private static int loadPortFromConfig(String configFile) {
        Properties prop = new Properties();
        // 기본 포트 번호 설정
        int defaultPort = 9999;
        try (FileInputStream input = new FileInputStream(configFile)) {
            // 파일 내용을 Properties 객체로 로드
            prop.load(input);
            // "PORT" 키에 해당하는 값 가져오기
            String portStr = prop.getProperty("PORT");
            // 문자열 포트를 정수로 변환하여 반환
            return Integer.parseInt(portStr);
        } catch (Exception e) { // 파일이 없거나 오류 발생 시
            System.out.println("'" + configFile + "' not found or invalid. Using default port " + defaultPort + ".");
            // 기본 포트 반환
            return defaultPort;
        }
    }

    // 메인 메소드
    public static void main(String[] args) {
        // 여러 클라이언트 요청을 처리하기 위한 스레드 풀 생성 (최대 10개)
        ExecutorService pool = Executors.newFixedThreadPool(10);
        
        try {
            // 설정 파일에서 포트 번호 불러오기
            int port = loadPortFromConfig("server.conf");
            // 서버 소켓 생성 및 포트 바인딩
            ServerSocket listener = new ServerSocket(port);
            System.out.println("Server is running on port " + port);

            // 무한 루프를 돌면서 클라이언트의 접속을 계속 대기
            while (true) {
                // 클라이언트 연결을 기다리고 수락 (새 연결이 올 때까지 여기서 멈춤)
                Socket socket = listener.accept();
                // 연결된 클라이언트를 처리할 작업을 스레드 풀에 제출
                pool.submit(new ClientHandler(socket));
            }
        } catch (IOException e) {
            System.out.println("Server Error: " + e.getMessage());
        }
    }
}

// 각 클라이언트와의 통신을 독립적으로 담당하는 클래스
class ClientHandler implements Runnable {
    // 통신을 위한 소켓 변수
    private final Socket socket;

    // 생성자: 연결된 클라이언트의 소켓을 받아와서 저장
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // 스레드가 실제로 실행할 코드
    @Override
    public void run() {
        System.out.println("Client connected");
        // 변수 선언
        BufferedReader in = null;
        BufferedWriter out = null;
        try {
            // 클라이언트와 통신하기 위한 입출력 스트림 생성
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
            // 클라이언트로부터 메시지를 계속 받기 위한 무한 루프
            while (true) {
                // 클라이언트로부터 한 줄의 메시지를 읽음
                String inputMessage = in.readLine();
                
                // 클라이언트 연결이 끊겼거나 "bye" 메시지를 받으면 루프 종료
                if (inputMessage == null || inputMessage.equalsIgnoreCase("bye")) {
                    break;
                }
                // 받은 메시지를 calc 메소드로 계산
                String res = Server.calc(inputMessage);
                // 계산 결과를 클라이언트에게 전송
                out.write(res + "\n");
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Error with client: " + e.getMessage());
        } finally {
            // 사용한 모든 리소스를 닫음
            try {
                if(in != null) in.close();
                if(out != null) out.close();
                if(socket != null) socket.close();
            } catch (IOException e) {
                System.out.println("Error closing client resources: " + e.getMessage());
            }
            System.out.println("Client disconnected");
        }
    }
}