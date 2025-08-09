const SharePage = () => {
    const [status, setStatus] = React.useState('loading'); // loading, requires_password, downloading, error
    const [password, setPassword] = React.useState('');
    const [errorMessage, setErrorMessage] = React.useState('');

    const getUrlKey = () => {
        const pathParts = window.location.pathname.split('/');
        return pathParts[pathParts.length - 1];
    };

    const urlKey = getUrlKey();

    const handleDownload = async (pass) => {
        try {
            await API.downloadSharedFile(urlKey, pass);
            setStatus('success');
        } catch (err) {
            setStatus('error');
            setErrorMessage(err.message);
        }
    };

    React.useEffect(() => {
        if (!urlKey) {
            setStatus('error');
            setErrorMessage('유효하지 않은 URL입니다.');
            return;
        }

        API.getShareLinkInfo(urlKey)
            .then(info => {
                if (info.requiresPassword) {
                    setStatus('requires_password');
                } else {
                    setStatus('downloading');
                    handleDownload(null);
                }
            })
            .catch(err => {
                setStatus('error');
                setErrorMessage(err.message);
            });
    }, []);

    const handleSubmit = (e) => {
        e.preventDefault();
        setStatus('downloading');
        handleDownload(password);
    };

    const renderContent = () => {
        switch (status) {
            case 'loading':
                return <p>링크 정보를 확인 중입니다...</p>;
            case 'requires_password':
                return (
                    <form onSubmit={handleSubmit}>
                        <h2 className="text-xl font-bold mb-4">비밀번호 입력</h2>
                        <p className="mb-4">이 파일은 비밀번호로 보호되어 있습니다.</p>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="비밀번호"
                            className="w-full border p-2 mb-4 rounded"
                        />
                        <button type="submit" className="w-full p-2 bg-blue-600 text-white rounded hover:bg-blue-700">
                            파일 다운로드
                        </button>
                    </form>
                );
            case 'downloading':
                return <p>다운로드를 시작합니다...</p>;
            case 'success':
                return <p className="text-green-600">다운로드가 시작되었습니다. 브라우저의 다운로드 창을 확인해주세요.</p>;
            case 'error':
                return <p className="text-red-600">오류: {errorMessage}</p>;
            default:
                return null;
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center">
            <div className="max-w-md w-full bg-white p-8 rounded-lg shadow-lg">
                {renderContent()}
            </div>
        </div>
    );
};

const container = document.getElementById("root");
const root = ReactDOM.createRoot(container);
root.render(<SharePage />);