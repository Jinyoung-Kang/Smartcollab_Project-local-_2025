const Realtime = {
    stompClient: null,
    connect: function(teamId, callbacks) {
        if (this.stompClient !== null) {
            this.disconnect();
        }
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);

            this.stompClient.subscribe(`/topic/team/${teamId}`, (message) => {
                if (callbacks.onChatMessage) callbacks.onChatMessage(JSON.parse(message.body));
            });

            this.stompClient.subscribe(`/topic/editor/${teamId}`, (message) => {
                if (callbacks.onEditorChange) callbacks.onEditorChange(JSON.parse(message.body));
            });

            const username = localStorage.getItem('username');
            this.stompClient.send(`/app/chat.addUser/${teamId}`,
                {},
                JSON.stringify({ sender: username, type: 'JOIN' })
            );
        }, (error) => {
            console.error('Connection error: ' + error);
        });
    },
    disconnect: function() {
        if (this.stompClient !== null) {
            this.stompClient.disconnect();
            this.stompClient = null;
            console.log("Disconnected");
        }
    },
    sendMessage: function(teamId, message) {
        if (this.stompClient && message.content) {
            this.stompClient.send(`/app/chat.sendMessage/${teamId}`, {}, JSON.stringify(message));
        }
    },
    sendEditorChange: function(teamId, changeData) {
        if (this.stompClient) {
            this.stompClient.send(`/app/editor.change/${teamId}`, {}, JSON.stringify(changeData));
        }
    },
};

const API = {
    login: async (username, password) => {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            throw new Error("아이디 또는 비밀번호가 일치하지 않습니다. 다시 확인해주세요.");
        }

        const data = await response.json();
        localStorage.setItem("token", data.accessToken);
        return data;
    },
    signup: async (username, password, name, email, passwordCheck) => {
        const response = await fetch("/api/auth/signup", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password, name, email, passwordCheck })
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "회원가입 실패");
        }
        return await response.text();
    },
    downloadFile: async (fileId, originalName) => {
        const token = localStorage.getItem("token");
        const response = await fetch(`/api/files/download-by-id/${fileId}`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });

        if (!response.ok) {
            throw new Error("파일 다운로드에 실패했습니다.");
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = originalName;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(downloadUrl);
    },
    createShareLink: async (fileId, options) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/share/${fileId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(options),
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "공유 링크 생성 실패");
        }
        return await res.text();
    },
    downloadSharedFile: async (urlKey, password) => {
        const url = password
            ? `/api/share/download/${urlKey}?password=${encodeURIComponent(password)}`
            : `/api/share/download/${urlKey}`;

        const response = await fetch(url);

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "파일 다운로드에 실패했습니다.");
        }

        const disposition = response.headers.get('Content-Disposition');
        let filename = 'downloaded_file';
        if (disposition && disposition.indexOf('attachment') !== -1) {
            const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
            const matches = filenameRegex.exec(disposition);
            if (matches != null && matches[1]) {
                filename = matches[1].replace(/['"]/g, '');
                filename = decodeURIComponent(filename);
            }
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(downloadUrl);
    },
    getShareLinkInfo: async (urlKey) => {
        const res = await fetch(`/api/share/info/${urlKey}`);
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "링크 정보 확인 실패");
        }
        return await res.json();
    },
    getCurrentUser: async () => {
        const token = localStorage.getItem("token");
        if (!token) throw new Error("토큰이 없습니다.");
        const response = await fetch("/api/auth/me", {
            headers: { Authorization: `Bearer ${token}` },
        });
        if (!response.ok) throw new Error("사용자 정보를 가져올 수 없습니다.");
        return await response.json();
    },
    uploadFile: async (options) => {
        const { file, folderId, teamId } = options;
        const formData = new FormData();
        formData.append("file", file);
        if(folderId) formData.append("folderId", folderId);
        if(teamId) formData.append("teamId", teamId);

        const token = localStorage.getItem("token");
        const response = await fetch("/api/files/upload", {
            method: "POST",
            headers: { Authorization: `Bearer ${token}` },
            body: formData,
        });
        if (!response.ok) throw new Error("파일 업로드 실패");
        return await response.json();
    },
    getFolders: async () => {
        const token = localStorage.getItem("token");
        const res = await fetch("/api/dashboard/root", {
            headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("폴더 목록을 가져올 수 없습니다.");
        return await res.json();
    },
    getFolderContents: async (folderId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/dashboard/folder/${folderId}`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("폴더 내용을 가져올 수 없습니다.");
        return await res.json();
    },
    createFolder: async (options) => {
        const { folderName, parentFolderId, teamId } = options;
        const token = localStorage.getItem("token");

        const requestBody = { folderName, parentFolderId, teamId };

        const res = await fetch(`/api/folders`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(requestBody),
        });

        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "폴더 생성 실패");
        }
        return await res.text();
    },
    deleteFile: async (fileId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/files/${fileId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "파일 삭제 실패");
        }
        return await res.text();
    },
    renameFile: async (fileId, newName) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/files/${fileId}/rename`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ newName }),
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "이름 변경 실패");
        }
        return await res.text();
    },
    renameFolder: async (folderId, newName) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/folders/${folderId}/rename`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ newName }),
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "폴더 이름 변경 실패");
        }
        return await res.text();
    },
    getFolderTree: async (teamId) => {
        const token = localStorage.getItem("token");
        const url = teamId ? `/api/folders/tree?teamId=${teamId}` : '/api/folders/tree';

        const res = await fetch(url, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("폴더 트리를 가져올 수 없습니다.");
        return await res.json();
    },
    moveItems: async (itemUniqueKeys, destinationFolderId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/files/move`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ itemUniqueKeys, destinationFolderId })
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "항목 이동 실패");
        }
        return await res.text();
    },
    copyItems: async (itemUniqueKeys, destinationFolderId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/files/copy`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ itemUniqueKeys, destinationFolderId })
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "항목 복사 실패");
        }
        return await res.text();
    },
    getMyTeams: async () => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/teams/my-teams`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("내 팀 목록을 가져올 수 없습니다.");
        return await res.json();
    },
    createTeam: async (teamName) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/teams`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ teamName }),
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "팀 생성 실패");
        }
        return await res.text();
    },
    leaveTeam: async (teamId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/teams/${teamId}/leave`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "팀 나가기 실패");
        }
        return await res.text();
    },
    deleteTeam: async (teamId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/teams/${teamId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "팀 삭제 실패");
        }
        return await res.text();
    },
    getTeamRootItems: async (teamId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/dashboard/team/${teamId}/root`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("팀 폴더 내용을 가져올 수 없습니다.");
        return await res.json();
    },
    getTeamMembers: async (teamId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/teams/${teamId}/members`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("팀 멤버 목록을 가져올 수 없습니다.");
        return await res.json();
    },
    inviteTeamMember: async (teamId, username) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/teams/${teamId}/invite`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ username }),
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "팀원 초대 실패");
        }
        return await res.text();
    },
    getNotifications: async () => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/notifications`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("알림을 가져올 수 없습니다.");
        return await res.json();
    },
    markNotificationAsRead: async (notificationId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/notifications/${notificationId}/read`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("알림을 읽음 처리할 수 없습니다.");
    },
    acceptInvitation: async (invitationId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/invitations/${invitationId}/accept`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "초대 수락 실패");
        }
        return await res.text();
    },
    rejectInvitation: async (invitationId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/invitations/${invitationId}/reject`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "초대 거절 실패");
        }
        return await res.text();
    },
    deleteNotification: async (notificationId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/notifications/${notificationId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("알림 삭제에 실패했습니다.");
    },
    deleteAllNotifications: async () => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/notifications/all`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("알림 전체 삭제에 실패했습니다.");
    },
    getChatHistory: async (teamId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/chat/${teamId}/history`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("채팅 기록을 가져올 수 없습니다.");
        return await res.json();
    },
    clearChatHistory: async (teamId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/chat/${teamId}/history`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) throw new Error("채팅 기록 삭제에 실패했습니다.");
    },
    removeMember: async (teamId, memberId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/teams/${teamId}/members/${memberId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "팀원 삭제 실패");
        }
        return await res.text();
    },
    updateMemberPermissions: async (teamId, memberId, permissions) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/teams/${teamId}/members/${memberId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(permissions),
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "권한 수정 실패");
        }
        return await res.text();
    },
    delegateLeadership: async (teamId, memberId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/teams/${teamId}/delegate/${memberId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "팀장 위임 실패");
        }
        return await res.text();
    },
    getFileContent: async (fileId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/files/${fileId}/content`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            throw new Error("파일 내용을 가져올 수 없습니다.");
        }
        return await res.text();
    },
    saveFileContent: async (fileId, content) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/files/save/${fileId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ content }),
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "파일 저장 실패");
        }
        return await res.text();
    },
    getVersionHistory: async (fileId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/files/${fileId}/history`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            throw new Error("버전 기록을 가져올 수 없습니다.");
        }
        return await res.json();
    },
    restoreVersion: async (fileId, versionId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/files/${fileId}/restore-version/${versionId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "버전 복원 실패");
        }
        return await res.text();
    },
    deleteFolder: async (folderId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/folders/${folderId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "폴더 삭제 실패");
        }
        return await res.text();
    },
    getFileBlobUrl: async (fileId) => {
        const token = localStorage.getItem("token");
        const response = await fetch(`/api/files/download-by-id/${fileId}`, {
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!response.ok) {
            throw new Error("파일을 불러오는데 실패했습니다.");
        }
        const blob = await response.blob();
        return window.URL.createObjectURL(blob);
    },
    summarizeFile: async (fileId) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/ai/summarize/${fileId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "요약 실패");
        }
        return await res.text();
    },
    translateFile: async (fileId, targetLanguage) => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/ai/translate/${fileId}?targetLanguage=${targetLanguage}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "번역 실패");
        }
        return await res.text();
    },
    deleteAccount: async () => {
        const token = localStorage.getItem("token");
        const res = await fetch(`/api/users/me`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` },
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "회원 탈퇴에 실패했습니다.");
        }
        return await res.text();
    },
    searchFiles: async (options) => {
        const { query, teamId } = options;
        const token = localStorage.getItem("token");

        let url = `/api/files/search?query=${encodeURIComponent(query)}`;
        if (teamId) {
            url += `&teamId=${teamId}`;
        }

        const res = await fetch(url, {
            headers: { 'Authorization': `Bearer ${token}` },
        });

        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "파일 검색에 실패했습니다.");
        }
        return await res.json();
    },
};