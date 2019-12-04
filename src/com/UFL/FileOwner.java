package com.UFL;
// package com.UFL;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class Constants {
    public static final String LIST = "LIST";
    public static final String NAME = "NAME";
    public static final String REQUEST = "REQUEST";
    public static final String DATA = "DATA";
    public static final String REGISTER = "REGISTER";
    public static final String PEER = "PEER";
    public static final String CLOSE = "CLOSE";
    public static final String BRACE_OPEN = "[";
    public static final String CONNECTION_ESTABLISHED = "] connection established from ";
    public static final String PEER_LIST = "[Owner] Peer list:";
    public static final String PEER_U_D = "[Owner] Transmitting Upload/Download peers";
    public static final String OWNER_UP_RUN = "[Owner] Owner is up and running:";
    public static final String OWNER_GETS_MESSAGE = "[Owner] gets message (";
    public static final String FROM = ") from ";
    public static final String ERROR_OCCURED = "Error Occurred";
    public static final String FILE_OWNER = "File Owner";
    public static final String WAITING_PEER = "is waiting for peers...";
    public static final String CONFIG = "CONFIG";
    public static final String OWNER_DIR = "OwnerDir";
    public static final String OWNER_DIR_1 = "OwnerDir/";
    public static final String BLOCK = "Block -->";
    public static final String BYTES = " bytes";
    public static final String OWNER_TOTAL = "[Owner] Total ";
    public static final String BLOCKS = " Blocks";
    public static final String EQUALS = " = ";
    public static final String SPACE = " ";
}

class OwnerProcess extends Thread {
    protected ObjectOutputStream object_output_stream;
    protected ObjectInputStream input_stream;

    public String peer_name = this.getName();

    protected HashMap<Integer, byte[]> current_block;

    protected String file_name = "";

    public void transferMessageToPeer(Object msg) throws IOException {
        this.object_output_stream.writeObject(msg);
        this.performFlush();
    }

    public void transferMessageToPeer(int msg) throws IOException {
        this.object_output_stream.writeInt(msg);
        this.performFlush();
    }

    public void performFlush() throws IOException {
        this.object_output_stream.flush();
        this.object_output_stream.reset();
    }

    public void initiateBlockPerFile(HashMap<Integer, byte[]> block) {
        this.current_block = block;
    }

    public void initiateNameForCurrentFile(String file_name) {
        this.file_name = file_name;
    }

    public void initiateCurrentNetwork(Socket socket,
                                       ObjectOutputStream _object_output_stream,
                                       ObjectInputStream _input_stream) {
        this.object_output_stream = _object_output_stream;
        this.input_stream = _input_stream;
        System.out.println(Constants.BRACE_OPEN + peer_name +
                Constants.CONNECTION_ESTABLISHED + socket.getPort());
    }

    protected int clientId = -1;

    private void replyToIndividualPeerRequest(String message, int x)
            throws IOException,
            ClassNotFoundException {
        switch (message) {
            case Constants.LIST:
                // transmit all the block
                performListOperation();
                break;
            case Constants.NAME:
                // transmit file name
                transferMessageToPeer((Object) this.file_name);
                break;
            case Constants.REQUEST:
                // Read block number as first input
                performRequestOperation(x);
                break;
            case Constants.DATA:
                x = this.input_stream.readInt();
                byte[] chunk = (byte[]) this.input_stream.readObject();
                break;
            case Constants.REGISTER:
                performRegisterOperation();
                break;
            case Constants.PEER:
                performPeerOperation();
                break;
            case Constants.CLOSE:
                this.performCloseOperation();
                return;

        }
    }

    private void performListOperation() throws IOException {
        ArrayList<Integer> arrayList = new ArrayList<Integer>(this.current_block.size());
        for (int x = 0; x < this.current_block.size(); x++) {
            if (this.current_block.containsKey(x)) {
                arrayList.add(x);
            }
        }
        transferMessageToPeer(arrayList);
    }

    private void performPeerOperation()
            throws IOException {
        System.out.print(Constants.PEER_LIST);
        int peer_id = input_stream.readInt();
        for (int _peer : FileOwner.list_peer.keySet()) {
            System.out.print(_peer + " ");
        }
        int peer_port = input_stream.readInt();
        int download_port = input_stream.readInt();
        System.out.println(peer_port + " " + download_port);
        System.out.println(Constants.PEER_U_D);
        transferMessageToPeer(FileOwner.list_peer);
        int downLoad_neighbor_id =
                FileOwner.master_DB.containsKey(download_port)
                        ? FileOwner.master_DB.get(download_port) : 0;
        transferMessageToPeer((Object) downLoad_neighbor_id);
        int upload_neighbor_id = getUploadNeighbor(peer_port);
        transferMessageToPeer((Object)upload_neighbor_id);
    }

    private int getUploadNeighbor(int peer_port) {
        int upload_neighbor_id = 0;
        for (Map.Entry<Integer, PortDB> entry : FileOwner.port_DB.entrySet()) {
            if (entry.getValue().getDownload_port() == peer_port) {
                int peer_id = entry.getValue().getPeer_port();
                upload_neighbor_id = FileOwner.master_DB.get(peer_id);
                break;
            }
        }
        return upload_neighbor_id;
    }

    private void performCloseOperation()
            throws IOException {
        object_output_stream.close();
        input_stream.close();
        FileOwner.list_peer.remove(clientId);
    }

    private void performRequestOperation(int x)
            throws IOException {
        x = this.input_stream.readInt();
        transferMessageToPeer(x);
        transferMessageToPeer(this.current_block.get(x));
    }

    private void performRegisterOperation()
            throws IOException {
        int peer = FileOwner.peer_id;
        FileOwner.list_peer.put(FileOwner.peer_id, FileOwner.port);
        System.out.println(peer);
        System.out.println(FileOwner.list_peer);
        int port = FileOwner.port;
        System.out.println(port);
        transferMessageToPeer(peer);
        transferMessageToPeer(port);

//        System.out.println("File: "+FileOwner.peer_id);
//        System.out.println("Port: "+FileOwner.port);
    }

    private String initiate_run() {
        System.out.println(Constants.OWNER_UP_RUN);
        Object input_from_peer;
        while (true) {
            try {
                input_from_peer = this.input_stream.readObject();
                break;
            } catch (Exception Ex) {

            }
        }
        String msg = (String) input_from_peer;
        System.out.println(Constants.OWNER_GETS_MESSAGE + msg + Constants.FROM + clientId);
        return msg;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String message = initiate_run();
                System.out.println(Constants.OWNER_GETS_MESSAGE + message + Constants.FROM + clientId);
                int x = -1;
                replyToIndividualPeerRequest(message, x);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                System.out.println(Constants.ERROR_OCCURED);
                FileOwner.list_peer.remove(clientId);
                return;
            }
        }
    }
}

class PortDB {
    private int peer_port;
    private int download_port;

    PortDB(int _peer_port, int _download_port) {
        peer_port = _peer_port;
        download_port = _download_port;
    }

    public int getDownload_port() {
        return download_port;
    }

    public int getPeer_port() {
        return peer_port;
    }
}

public class FileOwner {
    private String file_name = "";
    private int owner_port = 0;
    private ServerSocket owner_skt;
    public static int peer_id;
    public static int port;
    public static HashMap<Integer, Integer> list_peer = new HashMap<Integer, Integer>();
    public static HashMap<Integer, Integer> master_DB = new HashMap<Integer, Integer>();
    public static HashMap<Integer, PortDB> port_DB = new HashMap<Integer, PortDB>();


    // Peer configs
    public final int FILE_MAX_SIZE = 102400;
    public static HashMap<Integer, byte[]> file_block_list = new HashMap<Integer, byte[]>();
    public static String peerName = Constants.FILE_OWNER;

    public FileOwner(int _owner_port, String _file_name) {
        if (_file_name != null && new File(_file_name).exists()) {
            this.file_name = _file_name;
        }
        this.owner_port = _owner_port;
        try {
            owner_skt = new ServerSocket(this.owner_port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        // Init file chunk list
        this.divideFileIntoChunks();
    }

    public void initiateOwner() {
        try {
            while (true) {
                System.out.println(this.peerName + Constants.WAITING_PEER);
                Socket o_skt = owner_skt.accept();
                ObjectOutputStream objectOutputStream;
                ObjectInputStream inputStream;
                objectOutputStream = new ObjectOutputStream(o_skt.getOutputStream());
                inputStream = new ObjectInputStream(o_skt.getInputStream());
                String message_from_client = (String) inputStream.readObject();
                if (message_from_client.toUpperCase().startsWith(Constants.CONFIG)) {
                    String[] split_string = message_from_client.split("\\s+");
                    int id = Integer.parseInt(split_string[1]);
                    int peer_port = Integer.parseInt(split_string[2]);
                    int download_port = Integer.parseInt(split_string[3]);
                    this.peer_id = id;
                    this.port = peer_port;
                    createDB(id, peer_port, download_port);
                }
                OwnerProcess op = new OwnerProcess();
                op.initiateBlockPerFile(file_block_list);
                op.initiateNameForCurrentFile(this.file_name);
                op.initiateCurrentNetwork(o_skt, objectOutputStream, inputStream);
                op.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void createDB(int id, int peer_port, int download_port) {
        master_DB.put(peer_port, id);
        port_DB.put(id, new PortDB(peer_port, download_port));
    }

    private void divideFileIntoChunks() {
        try {
            File sepDir = new File(Constants.OWNER_DIR);
            if (!sepDir.exists()) {
                sepDir.mkdir();
            }
            FileInputStream fileInputStream = new FileInputStream(this.file_name);
            byte[] buffer = new byte[this.FILE_MAX_SIZE];
            int id = 0;
            int character;
            while ((character = fileInputStream.read(buffer)) != -1) {
                perFormFileChunks(buffer, character, id);
                buffer = new byte[this.FILE_MAX_SIZE];
                id++;
            }
            System.out.println(Constants.OWNER_TOTAL + id + Constants.BLOCKS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void perFormFileChunks(byte[] buffer, int character, int id) throws IOException {
        byte[] bChunk = Arrays.copyOfRange(buffer, 0, character);
        file_block_list.put(id, bChunk);
        System.out.println(Constants.BLOCK + id + Constants.EQUALS + character + Constants.BYTES);
        FileOutputStream fso = new FileOutputStream(Constants.OWNER_DIR_1 + id, false);
        fso.write(bChunk);
        fso.flush();
        fso.close();
    }

    private static void testConfig() {
//        createConfig(2, 9001, 9005);
//        createConfig(3, 9002, 9001);
//        createConfig(4, 9003, 9002);
//        createConfig(5, 9004, 9003);
//        createConfig(6, 9005, 9004);

//        for (int name : config_peer.keySet()) {
//            PeerConfigModel peerConfigModel = config_peer.get(name);
//            System.out.println(name + " " + peerConfigModel.getPort() + " " +
//                    peerConfigModel.getDownLoadId() + " " +
//                    peerConfigModel.getUploadId());
//        }
    }

    public static void main(String[] args) {
        int owner_port = 0;
        String file_name = "/Users/shantanughosh/Desktop/Shantanu_MS/Fall_19/CN/Projects/P2p_Final/GitHub/Bittorrent-CN/TCP-connection-1.pdf";
        if (args.length > 0) {
            owner_port = Integer.parseInt(args[0]);
            // file_name = args[1];
        }
        System.out.println("");
        System.out.println("Owner Port: " + owner_port + " File Name: " + file_name);
        new FileOwner(owner_port, file_name).initiateOwner();
    }
}