package com.example.sharenow;

import java.io.*;
import java.net.Socket;

public class NetworkUtil implements Serializable {
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public NetworkUtil(String s, int port) throws IOException {
        this.socket = new Socket(s, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public NetworkUtil(Socket s) throws IOException {
        this.socket = s;
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public Object read() throws IOException, ClassNotFoundException {
        return ois.readUnshared();
    }

    public void write(Object o) throws IOException {
        oos.reset();
        oos.writeUnshared(o);
    }
    public void closeConnection() throws IOException {
        ois.close();
        oos.close();
        socket.close();
    }
}
/*
import java.io.*;
import java.net.Socket;

public class NetworkUtil implements Serializable {
    private Socket socket;
    private DataOutputStream oos;
    private DataInputStream ois;

    public NetworkUtil(String s, int port) throws IOException {
        this.socket = new Socket(s, port);
        oos = new DataOutputStream(socket.getOutputStream());
        ois = new DataInputStream(socket.getInputStream());
    }

    public NetworkUtil(Socket s) throws IOException {
        this.socket = s;
        oos = new DataOutputStream(socket.getOutputStream());
        ois = new DataInputStream(socket.getInputStream());
    }

    public void read(byte[] bytes) throws IOException, ClassNotFoundException {
        ois.read(bytes);
    }

    public void write(byte[] bytes) throws IOException {
        oos.write(bytes);
    }
    public int readInt() throws IOException, ClassNotFoundException {
        return ois.readInt();
    }

    public void writeInt(int size) throws IOException {
        oos.writeInt(size);
    }
    public void closeConnection() throws IOException {
        ois.close();
        oos.close();
    }
}


 */

