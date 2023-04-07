package de.hska.iwi.vislab.lab1.example.ws;

import javax.jws.WebService;

@WebService(endpointInterface = "de.hska.iwi.vislab.lab1.example.ws.FibonacciServiceIntf")
public class FibonacciServiceImpl implements FibonacciServiceIntf{
    public int getFibonacci(int n) {
        if(n < 2) return n;
        return getFibonacci(n - 1) + getFibonacci(n - 2);
    }
}
