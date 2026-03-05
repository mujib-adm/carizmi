export default function Footer() {
  const year = new Date().getFullYear();
  return (
    <footer className="copyright">
      <p>© {year} Sof'umar Community of Minnesota.</p>
    </footer>
  );
}