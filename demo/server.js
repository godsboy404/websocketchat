const fs = require('fs');
const WebSocket = require('ws');
const server = new WebSocket.Server({ port: 3000 });

const messagesFile = 'messages.json';
let messages = [];
let users = {}; // 存储连接的用户

// 读取历史消息
if (fs.existsSync(messagesFile)) {
    messages = JSON.parse(fs.readFileSync(messagesFile));
}

server.on('connection', ws => {
    let userName = null;
    let room = '公共群聊';

    ws.on('message', message => {
        const msg = JSON.parse(message);

        if (msg.type === 'join') {
            userName = msg.user;
            room = msg.room || '公共群聊';
            users[userName] = ws;
            console.log(`🔵 ${userName} 加入 ${room}`);
            ws.send(JSON.stringify({ type: 'history', data: messages.filter(m => m.room === room) }));
            broadcastUserList(); // 发送更新后的用户列表
            return;
        }

        if (msg.type === 'message') {
            const chatMessage = { user: userName, message: msg.message, room: room, private: msg.private || false, to: msg.to || null };
            messages.push(chatMessage);
            fs.writeFileSync(messagesFile, JSON.stringify(messages, null, 2));

            server.clients.forEach(client => {
                if (client.readyState === WebSocket.OPEN) {
                    if (!msg.private || (msg.private && msg.to && users[msg.to] === client) || userName === msg.to) {
                        client.send(JSON.stringify({ type: 'message', data: chatMessage }));
                    }
                }
            });
        }
    });

    ws.on('close', () => {
        if (userName) {
            delete users[userName];
            console.log(`🔴 ${userName} 断开连接`);
            broadcastUserList(); // 用户离开时更新用户列表
        }
    });
});

// 发送当前在线用户列表
function broadcastUserList() {
    const userList = Object.keys(users);
    server.clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify({ type: 'userList', users: userList }));
        }
    });
}

console.log('✅ WebSocket 服务器运行在 ws://localhost:3000');