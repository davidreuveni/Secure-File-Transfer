window.secureftRtcFile = {
  initFileState(ctx) {
    ctx.receivingFileMeta = null;
    ctx.receivedChunks = [];
    ctx.receivedSize = 0;
    ctx.outgoingFileMeta = null;
  },

  resetFileState(ctx) {
    ctx.receivingFileMeta = null;
    ctx.receivedChunks = [];
    ctx.receivedSize = 0;
    ctx.outgoingFileMeta = null;
  },

  handleIncomingData(ctx, data) {
    if (typeof data === "string") {
      if (data.startsWith("{")) {
        const msg = JSON.parse(data);

        if (msg.type === "file-meta") {
          ctx.receivingFileMeta = msg;
          ctx.receivedChunks = [];
          ctx.receivedSize = 0;
          ctx.host.$server.appendSystemMessage(
            "Receiving file: " + msg.name + " (" + msg.size + " bytes)"
          );
          ctx.host.$server.updateTransferProgress(
            "Receiving " + msg.name,
            0
          );
          return true;
        }

        if (msg.type === "file-complete") {
          if (ctx.receivingFileMeta && ctx.receivedChunks.length > 0) {
            const fullBytes = new Uint8Array(ctx.receivedSize);
            let offset = 0;

            for (const chunk of ctx.receivedChunks) {
              fullBytes.set(chunk, offset);
              offset += chunk.length;
            }

            this.downloadReceivedFile(
              ctx.receivingFileMeta.name,
              ctx.receivingFileMeta.mime,
              fullBytes
            );

            ctx.host.$server.appendSystemMessage(
              "File received: " + ctx.receivingFileMeta.name
            );
            ctx.host.$server.updateTransferProgress(
              "Received " + ctx.receivingFileMeta.name,
              1
            );
            setTimeout(() => ctx.host.$server.hideTransferProgress("No active transfer"), 1200);
          }

          this.resetFileState(ctx);
          return true;
        }
      }

      return false;
    }

    const chunk = new Uint8Array(data);
    ctx.receivedChunks.push(chunk);
    ctx.receivedSize += chunk.length;
    if (ctx.receivingFileMeta && ctx.receivingFileMeta.size > 0) {
      ctx.host.$server.updateTransferProgress(
        "Receiving " + ctx.receivingFileMeta.name,
        ctx.receivedSize / ctx.receivingFileMeta.size
      );
    }
    return true;
  },

  async sendFile(ctx, fileName, mimeType, base64Data) {
    if (!ctx.dc || ctx.dc.readyState !== "open") {
      ctx.host.$server.updateStatus("Data channel is not open", true);
      return;
    }

    const binaryString = atob(base64Data);
    const bytes = new Uint8Array(binaryString.length);

    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }

    const chunkSize = 16 * 1024;
    const maxBufferedAmount = 256 * 1024;

    const meta = {
      type: "file-meta",
      name: fileName,
      mime: mimeType,
      size: bytes.length,
      chunkSize: chunkSize
    };

    ctx.outgoingFileMeta = meta;
    ctx.host.$server.updateTransferProgress("Sending " + fileName, 0);
    ctx.dc.send(JSON.stringify(meta));

    for (let offset = 0; offset < bytes.length; offset += chunkSize) {
      while (ctx.dc.bufferedAmount > maxBufferedAmount) {
        await new Promise(resolve => setTimeout(resolve, 20));
      }

      const chunk = bytes.slice(offset, offset + chunkSize);
      ctx.dc.send(chunk.buffer);
      ctx.host.$server.updateTransferProgress(
        "Sending " + fileName,
        Math.min(1, (offset + chunk.length) / bytes.length)
      );
    }

    ctx.dc.send(JSON.stringify({ type: "file-complete" }));
    ctx.host.$server.appendSystemMessage("File sent: " + fileName);
    ctx.host.$server.updateTransferProgress("Sent " + fileName, 1);
    setTimeout(() => ctx.host.$server.hideTransferProgress("No active transfer"), 1200);
  },

  downloadReceivedFile(name, mimeType, bytes) {
    const blob = new Blob([bytes], { type: mimeType || "application/octet-stream" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");

    a.href = url;
    a.download = name || "download";
    document.body.appendChild(a);
    a.click();
    a.remove();

    URL.revokeObjectURL(url);
  }
};
