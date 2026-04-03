window.secureftRtc = {
  init(host, username) {
    this.host = host;
    this.username = username;
    this.ws = null;
    this.pc = null;
    this.dc = null;
    this.currentPeer = null;

    window.secureftRtcFile.initFileState(this);

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    this.ws = new WebSocket(protocol + "//" + window.location.host + "/signal");

    this.ws.onopen = () => {
      this.host.$server.appendSystemMessage("WebSocket connected");
      this.ws.send(JSON.stringify({
        type: "register",
        from: this.username
      }));
    };

    this.ws.onmessage = async (e) => {
      try {
        this.host.$server.appendSystemMessage("WS raw message: " + e.data);

        if (typeof e.data === "string" && !e.data.startsWith("{")) {
          this.host.$server.appendSystemMessage(e.data);
          return;
        }

        const msg = JSON.parse(e.data);
        this.host.$server.appendSystemMessage("WS parsed type: " + msg.type);

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

          this.host.$server.appendSystemMessage("Answer sent to " + msg.from);
          return;
        }

        if (msg.type === "answer") {
          this.host.$server.appendSystemMessage("Processing answer from " + msg.from);

          if (!this.pc) {
            this.host.$server.appendSystemMessage("No peer connection exists for answer");
            return;
          }

          await this.pc.setRemoteDescription({
            type: "answer",
            sdp: msg.sdp
          });

          this.host.$server.appendSystemMessage("Remote answer received");
          return;
        }

        if (msg.type === "ice-candidate") {
          this.host.$server.appendSystemMessage("Processing ICE candidate from " + msg.from);

          if (!this.pc) {
            this.host.$server.appendSystemMessage("No peer connection exists for ICE candidate");
            return;
          }

          await this.pc.addIceCandidate({
            candidate: msg.candidate,
            sdpMid: msg.sdpMid,
            sdpMLineIndex: msg.sdpMLineIndex
          });

          this.host.$server.appendSystemMessage("Received ICE candidate from " + msg.from);
        }
      } catch (err) {
        this.host.$server.appendSystemMessage("WS handler error: " + err);
      }
    };

    this.ws.onerror = () => {
      this.host.$server.updateStatus("WebSocket error", true);
    };

    this.ws.onclose = () => {
      this.host.$server.appendSystemMessage("WebSocket closed");
    };

    this.host.$server.appendSystemMessage("RTC init for user: " + username);
    this.host.$server.updateStatus("RTC client initialized", false);
  },

  async createPeerConnection(isCaller) {
    if (this.dc) {
      try { this.dc.close(); } catch (e) {}
      this.dc = null;
    }

    if (this.pc) {
      try { this.pc.close(); } catch (e) {}
      this.pc = null;
    }

    window.secureftRtcFile.resetFileState(this);

    this.pc = new RTCPeerConnection({
      iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
    });

    this.pc.onicecandidate = (event) => {
      if (event.candidate && this.currentPeer) {
        this.host.$server.appendSystemMessage("Sending ICE candidate to " + this.currentPeer);

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
      this.host.$server.appendSystemMessage("Peer connection state: " + this.pc.connectionState);

      if (this.pc.connectionState === "failed" || this.pc.connectionState === "closed") {
        this.host.$server.updateStatus("Connection failed", true);
      }
    };

    this.pc.oniceconnectionstatechange = () => {
      this.host.$server.appendSystemMessage("ICE connection state: " + this.pc.iceConnectionState);

      if (this.pc.iceConnectionState === "failed") {
        this.host.$server.updateStatus("ICE failed", true);
      }
    };

    this.pc.onicegatheringstatechange = () => {
      this.host.$server.appendSystemMessage("ICE gathering state: " + this.pc.iceGatheringState);
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
      this.host.$server.appendSystemMessage("Data channel OPEN");
      this.host.$server.updateStatus("Connected to " + this.currentPeer, false);
    };

    channel.onclose = () => {
      this.host.$server.appendSystemMessage("Data channel CLOSED");
    };

    channel.onerror = () => {
      this.host.$server.appendSystemMessage("Data channel ERROR");
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

    this.host.$server.appendSystemMessage("Start call requested to: " + targetUsername);
    this.host.$server.updateStatus("Calling " + targetUsername + "...", false);

    this.ws.send(JSON.stringify({
      type: "call-user",
      from: this.username,
      to: targetUsername
    }));

    await this.createPeerConnection(true);

    this.host.$server.appendSystemMessage("Creating offer...");
    const offer = await this.pc.createOffer();

    this.host.$server.appendSystemMessage("Setting local offer...");
    await this.pc.setLocalDescription(offer);

    this.host.$server.appendSystemMessage("Local offer set");

    this.ws.send(JSON.stringify({
      type: "offer",
      from: this.username,
      to: targetUsername,
      sdp: offer.sdp
    }));

    this.host.$server.appendSystemMessage("Offer sent to " + targetUsername);
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

  hangUp() {
    if (this.dc) {
      try { this.dc.close(); } catch (e) {}
      this.dc = null;
    }

    if (this.pc) {
      try { this.pc.close(); } catch (e) {}
      this.pc = null;
    }

    window.secureftRtcFile.resetFileState(this);
    this.currentPeer = null;

    this.host.$server.appendSystemMessage("Connection closed");
    this.host.$server.updateStatus("Disconnected", false);
  }
};
