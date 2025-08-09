const App = () => {
    const [user, setUser] = React.useState(null);
    React.useEffect(() => {
        API.getCurrentUser()
            .then(setUser)
            .catch(() => setUser(null));
    }, []);

    if (!user) return <AuthForm onLogin={() => window.location.reload()} />;

    return <Dashboard user={user} />;
};

// [수정] React 18 방식인 createRoot API 사용
const container = document.getElementById("root");
const root = ReactDOM.createRoot(container);
root.render(<App />);