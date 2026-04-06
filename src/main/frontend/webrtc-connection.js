window.secureftRtc = {
  async init(host, username) {
    this.host = host;
    this.username = username;
    this.ws = null;
    this.pc = null;
    this.dc = null;
    this.currentPeer = null;
    this.peerPublicKey = null;
    this.rsaKeyPair = null;
    this.pendingSecret = null;

    window.secureftRtcFile.initFileState(this);

    this.ws = new WebSocket("ws://localhost:8080/signal");

    this.ws.onopen = () => {
      this.ws.send(JSON.stringify({
        type: "register",
        from: this.username
      }));
    };

    this.ws.onmessage = async (e) => {
      try {
        if (typeof e.data === "string" && !e.data.startsWith("{")) {
          return;
        }

        const msg = JSON.parse(e.data);

        if (msg.type === "call-user") {
          this.currentPeer = msg.from;
          this.host.$server.appendSystemMessage("Incoming call from: " + msg.from);
          this.host.$server.updateStatus("Incoming call from " + msg.from, false);
          return;
        }

        if (msg.type === "offer") {
          this.currentPeer = msg.from;

          await this.createPeerConnection(false);

          await this.pc.setRemoteDescription({
            type: "offer",
            sdp: msg.sdp
          });

          const answer = await this.pc.createAnswer();
          await this.pc.setLocalDescription(answer);

          this.ws.send(JSON.stringify({
            type: "answer",
            from: this.username,
            to: msg.from,
            sdp: answer.sdp
          }));
          return;
        }

        if (msg.type === "getkey") {
          this.currentPeer = msg.from;

          if (!this.rsaKeyPair) {
            this.rsaKeyPair = await crypto.subtle.generateKey(
              {
                name: "RSA-OAEP",
                modulusLength: 2048,
                publicExponent: new Uint8Array([1, 0, 1]),
                hash: "SHA-256"
              },
              true,
              ["encrypt", "decrypt"]
            );
          }

          const publicKeySpki = await crypto.subtle.exportKey("spki", this.rsaKeyPair.publicKey);

          const publicKeyBase64 = this.arrayBufferToBase64(publicKeySpki);

          this.ws.send(JSON.stringify({
            type: "herekey",
            from: this.username,
            to: msg.from,
            key: publicKeyBase64
          }));
          return;
        }

        if (msg.type === "herekey") {
          this.currentPeer = msg.from;
          const publicKeyBase64 = msg.key;

          const importedPublicKey = await crypto.subtle.importKey(
            "spki",
            this.base64ToArrayBuffer(publicKeyBase64),
            {
              name: "RSA-OAEP",
              hash: "SHA-256"
            },
            true,
            ["encrypt"]
          );

          this.peerPublicKey = importedPublicKey;

          this.host.$server.appendSystemMessage("Peer public key received");

          if (this.pendingSecret) {
            const pendingSecret = this.pendingSecret;
            this.pendingSecret = null;
            await this.exchangeSecretWithRSA(pendingSecret, true);
          }
          return;
        }

        if (msg.type === "secret-encrypted") {
          this.currentPeer = msg.from;
          const encryptedSecretBase64 = msg.encryptedValue;

          if (!this.rsaKeyPair) {
            this.host.$server.appendSystemMessage("No RSA key pair available to decrypt");
            return;
          }

          const decryptedSecret = await crypto.subtle.decrypt(
            { name: "RSA-OAEP" },
            this.rsaKeyPair.privateKey,
            this.base64ToArrayBuffer(encryptedSecretBase64)
          );

          const decoder = new TextDecoder();
          const secret = decoder.decode(decryptedSecret);

          this.host.$server.appendRemoteMessage(this.currentPeer, "[SECRET-RSA]: " + secret);
          return;
        }

        if (msg.type === "answer") {
          if (!this.pc) {
            this.host.$server.updateStatus("Received an answer without an active connection", true);
            return;
          }

          await this.pc.setRemoteDescription({
            type: "answer",
            sdp: msg.sdp
          });
          return;
        }

        if (msg.type === "ice-candidate") {
          if (!this.pc) {
            this.host.$server.updateStatus("Received network data without an active connection", true);
            return;
          }

          await this.pc.addIceCandidate({
            candidate: msg.candidate,
            sdpMid: msg.sdpMid,
            sdpMLineIndex: msg.sdpMLineIndex
          });
        }
      } catch (err) {
        this.host.$server.appendSystemMessage("WebRTC signaling error");
        this.host.$server.updateStatus("WebRTC signaling error", true);
      }
    };

    this.ws.onerror = () => {
      this.host.$server.hideTransferProgress("No active transfer");
      this.host.$server.appendSystemMessage("Signaling server error");
      this.host.$server.updateStatus("Signaling server error", true);
    };

    this.ws.onclose = () => {
      this.host.$server.hideTransferProgress("No active transfer");
      this.host.$server.appendSystemMessage("Signaling server disconnected");
      this.host.$server.updateStatus("Signaling server disconnected", true);
    };

    this.host.$server.updateStatus("Ready to connect", false);
  },

  async createPeerConnection(isCaller) {
    if (this.dc) {
      try { this.dc.close(); } catch (e) { }
      this.dc = null;
    }

    if (this.pc) {
      try { this.pc.close(); } catch (e) { }
      this.pc = null;
    }

    window.secureftRtcFile.resetFileState(this);

    this.pc = new RTCPeerConnection({
      iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
    });

    this.pc.onicecandidate = (event) => {
      if (event.candidate && this.currentPeer) {
        this.ws.send(JSON.stringify({
          type: "ice-candidate",
          from: this.username,
          to: this.currentPeer,
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid,
          sdpMLineIndex: event.candidate.sdpMLineIndex
        }));
      }
    };

    this.pc.onconnectionstatechange = () => {
      if (this.pc.connectionState === "failed" || this.pc.connectionState === "closed") {
        this.host.$server.updateStatus("Connection failed", true);
        this.host.$server.appendSystemMessage("Connection failed");
      }
    };

    this.pc.oniceconnectionstatechange = () => {
      if (this.pc.iceConnectionState === "failed") {
        this.host.$server.updateStatus("Connection failed", true);
        this.host.$server.appendSystemMessage("Connection failed");
      }
    };

    if (isCaller) {
      this.dc = this.pc.createDataChannel("chat");
      this.attachDataChannelHandlers(this.dc);
    } else {
      this.pc.ondatachannel = (event) => {
        this.dc = event.channel;
        this.attachDataChannelHandlers(this.dc);
      };
    }
  },

  attachDataChannelHandlers(channel) {
    channel.onopen = () => {
      this.host.$server.hideTransferProgress("No active transfer");
      this.host.$server.updateStatus("Connected to " + this.currentPeer, false);
      this.host.$server.appendSystemMessage("Connected to " + this.currentPeer);
    };

    channel.onclose = () => {
      this.host.$server.appendSystemMessage("Connection closed");
    };

    channel.onerror = () => {
      this.host.$server.hideTransferProgress("No active transfer");
      this.host.$server.appendSystemMessage("Data channel error");
      this.host.$server.updateStatus("Data channel error", true);
    };

    channel.onmessage = (ev) => {
      const handled = window.secureftRtcFile.handleIncomingData(this, ev.data);
      if (!handled) {
        this.host.$server.appendRemoteMessage(this.currentPeer, ev.data);
      }
    };
  },

  async startCall(targetUsername) {
    if (!this.host || !this.ws || this.ws.readyState !== WebSocket.OPEN) {
      this.host?.$server.updateStatus("WebSocket is not connected", true);
      return;
    }

    this.hangUp();

    this.currentPeer = targetUsername;

    this.host.$server.showTransferProgress("Connecting to " + targetUsername, true);
    this.host.$server.updateStatus("Calling " + targetUsername + "...", false);

    this.ws.send(JSON.stringify({
      type: "call-user",
      from: this.username,
      to: targetUsername
    }));

    await this.createPeerConnection(true);

    const offer = await this.pc.createOffer();
    await this.pc.setLocalDescription(offer);

    this.ws.send(JSON.stringify({
      type: "offer",
      from: this.username,
      to: targetUsername,
      sdp: offer.sdp
    }));
  },

  sendMessage(messageText) {
    if (!this.dc || this.dc.readyState !== "open") {
      this.host.$server.updateStatus("Data channel is not open", true);
      return;
    }

    this.dc.send(messageText);
  },

  async sendFile(fileName, mimeType, base64Data) {
    await window.secureftRtcFile.sendFile(this, fileName, mimeType, base64Data);
  },

  async exchangeSecretWithRSA(secret, skipKeyRequest = false) {
    if (!this.peerPublicKey) {
      if (!this.currentPeer) {
        this.host.$server.updateStatus("No peer connected", true);
        return;
      }

      if (skipKeyRequest) {
        this.host.$server.updateStatus("No peer public key available", true);
        return;
      }

      this.pendingSecret = secret;
      this.host.$server.updateStatus("Requesting peer public key...", false);
      this.ws.send(JSON.stringify({
        type: "getkey",
        from: this.username,
        to: this.currentPeer
      }));
      return;
    }

    if (!this.currentPeer) {
      this.host.$server.updateStatus("No peer connected", true);
      return;
    }

    const encoder = new TextEncoder();
    const secretBytes = encoder.encode(secret);

    const encryptedSecret = await crypto.subtle.encrypt(
      { name: "RSA-OAEP" },
      this.peerPublicKey,
      secretBytes
    );

    const encryptedSecretBase64 = this.arrayBufferToBase64(encryptedSecret);

    this.ws.send(JSON.stringify({
      type: "secret-encrypted",
      from: this.username,
      to: this.currentPeer,
      encryptedValue: encryptedSecretBase64
    }));

    this.host.$server.appendSystemMessage("Secret encrypted and sent with RSA");
  },

  hangUp() {
    if (this.dc) {
      try { this.dc.close(); } catch (e) { }
      this.dc = null;
    }

    if (this.pc) {
      try { this.pc.close(); } catch (e) { }
      this.pc = null;
    }

    window.secureftRtcFile.resetFileState(this);
    this.currentPeer = null;
    this.peerPublicKey = null;
    this.rsaKeyPair = null;
    this.pendingSecret = null;

    this.host.$server.hideTransferProgress("No active transfer");
    this.host.$server.updateStatus("Disconnected", false);
  },

  arrayBufferToBase64(buffer) {
    const bytes = new Uint8Array(buffer);
    let binary = "";
    for (const b of bytes) binary += String.fromCharCode(b);
    return btoa(binary);
  },
  base64ToArrayBuffer(base64) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
  }

};
