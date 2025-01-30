import { Client } from '@stomp/stompjs';

class ServiceHealthWebSocket {
    constructor() {
        this.subscribers = new Set();
        this.connectionSubscribers = new Set();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.reconnectDelay = 2000; // Start with 2 seconds
        this.maxReconnectDelay = 30000; // Max 30 seconds
        this.wsUrl = import.meta.env.VITE_WS_URL || 'wss://localhost:7777/ws-lite-mesh';
        this.ws = null;
        this.connected = false;
        this.connectionStatus = 'disconnected';
    }

    setConnectionStatus(status) {
        this.connectionStatus = status;
        this.connectionSubscribers.forEach(callback => callback(status));
    }

    onConnectionChange(callback) {
        this.connectionSubscribers.add(callback);
        callback(this.connectionStatus);
    }

    offConnectionChange(callback) {
        this.connectionSubscribers.delete(callback);
    }

    connect() {
        if (this.connected) return;

        try {
            console.log(`Attempting to connect to ${this.wsUrl}`);
            this.setConnectionStatus('connecting');
            
            this.ws = new WebSocket(this.wsUrl);
            this.setupWebSocket();
        } catch (error) {
            console.error('WebSocket connection error:', error);
            this.handleReconnect();
        }
    }

    setupWebSocket() {
        this.ws.onopen = () => {
            console.log('WebSocket Connected');
            this.reconnectAttempts = 0;
            
            // Send STOMP CONNECT frame
            setTimeout(() => {
                if (this.ws.readyState === WebSocket.OPEN) {
                    const connectFrame = 'CONNECT\n' +
                        'accept-version:1.1,1.0\n' +
                        'heart-beat:4000,4000\n' +
                        '\n\0';
                    this.ws.send(connectFrame);
                }
            }, 100);
        };

        this.ws.onmessage = (event) => {
            //console.log('Received raw message:', event.data);
            const frame = this.parseStompFrame(event.data);
            //console.log('Parsed frame:', frame);
            
            if (frame.command === 'CONNECTED') {
                console.log('STOMP Connected');
                this.connected = true;
                this.setConnectionStatus('connected');
                // Subscribe to health updates
                const subscribeFrame = 'SUBSCRIBE\n' +
                    'id:sub-0\n' +
                    'destination:/topic/health\n' +
                    '\n\0';
                //console.log('Sending subscribe frame:', subscribeFrame);
                this.ws.send(subscribeFrame);
            }
            else if (frame.command === 'MESSAGE') {
                try {
                    // Parse JSON directly
                    const payload = JSON.parse(frame.body);
                    //console.log('Parsed message payload:', payload);
                    this.notifySubscribers(payload);
                } catch (error) {
                    //console.error('Error parsing message payload:', error);
                }
            }
            else if (frame.command === 'RECEIPT') {
                //console.log('Received receipt:', frame);
            }
        };

        this.ws.onerror = (error) => {
            //console.error('WebSocket Error:', error);
            this.connected = false;
            this.setConnectionStatus('error');
            this.handleReconnect();
        };

        this.ws.onclose = () => {
            //console.log('WebSocket Closed');
            this.connected = false;
            this.setConnectionStatus('disconnected');
            this.handleReconnect();
        };
    }

    parseStompFrame(data) {
        const lines = data.split('\n');
        const command = lines[0];
        const headers = {};
        let body = '';
        let i = 1;

        // Parse headers
        while (i < lines.length && lines[i]) {
            const colonIndex = lines[i].indexOf(':');
            if (colonIndex > 0) {
                const key = lines[i].substring(0, colonIndex);
                const value = lines[i].substring(colonIndex + 1);
                headers[key] = value;
            }
            i++;
        }

        // Skip empty line after headers
        i++;

        // Get body - collect all remaining lines
        body = lines.slice(i).join('\n');

        // Remove null terminator and trailing newlines
        body = body.replace(/\0$/, '');

        return { command, headers, body };
    }

    handleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            const delay = Math.min(
                this.reconnectDelay * Math.pow(1.5, this.reconnectAttempts),
                this.maxReconnectDelay
            );
            
            setTimeout(() => {
                this.reconnectAttempts++;
                this.connect();
            }, delay);
            
            this.connectionStatus = 'reconnecting';
            this.notifyConnectionSubscribers();
        } else {
            this.connectionStatus = 'failed';
            this.notifyConnectionSubscribers();
        }
    }

    subscribe(callback) {
        this.subscribers.add(callback);
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            this.connect();
        }
        return () => this.subscribers.delete(callback);
    }

    notifySubscribers(data) {
        this.subscribers.forEach(callback => callback(data));
    }

    notifyConnectionSubscribers() {
        this.connectionSubscribers.forEach(callback => callback(this.connectionStatus));
    }

    disconnect() {
        if (this.ws) {
            if (this.connected) {
                const disconnectFrame = 'DISCONNECT\n\n\0';
                this.ws.send(disconnectFrame);
            }
            this.ws.close();
        }
    }
}

export const serviceHealthWebSocket = new ServiceHealthWebSocket(); 