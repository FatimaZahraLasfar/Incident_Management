export default function Loader({ label = 'Chargement…' }) {
  return <p className="loader" role="status">{label}</p>;
}
