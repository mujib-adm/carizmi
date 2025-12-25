import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Sidebar from "../../component/Sidebar";
import "./Dashboard.css";

type Metrics = {
  total: number;
  active: number;
  overdue: number;
  disqualified: number;
};

type Payment = {
  id: number;
  memberName: string;
  feeType: string;
  amount: number;
  date: string;
};

export default function Dashboard() {
  const [metrics, setMetrics] = useState<Metrics>({ total: 0, active: 0, overdue: 0, disqualified: 0 });
  const [payments, setPayments] = useState<Payment[]>([]);

  useEffect(() => {
    fetch("/api/dashboard/metrics", {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` }
    })
      .then(res => res.json())
      .then(setMetrics)
      .catch(err => console.error("Metrics fetch failed", err));

    fetch("/api/payments/latest", {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` }
    })
      .then(res => res.json())
      .then(setPayments)
      .catch(err => console.error("Payments fetch failed", err));
  }, []);

  return (
    <div className="dashboard">
      <Sidebar />

      <main className="content">
        <h1>Dashboard</h1>
        <div className="metrics">
          <MetricCard title="Total Members" value={metrics.total} />
          <MetricCard title="Active Members" value={metrics.active} />
          <MetricCard title="Overdue Members" value={metrics.overdue} />
          <MetricCard title="Disqualified Members" value={metrics.disqualified} />
        </div>

        <h1>Latest Payments</h1>
        <table className="latest-payments">
          <thead>
            <tr>
              <th>Member Name</th>
              <th>Fee Type</th>
              <th>Amount</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {payments.map(p => (
              <tr key={p.id}>
                <td><Link to={`/payments/${p.id}`}>{p.memberName}</Link></td>
                <td>{p.feeType}</td>
                <td>${p.amount.toFixed(2)}</td>
                <td>{new Date(p.date).toLocaleDateString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </main>
    </div>
  );
}

function MetricCard({ title, value }: { title: string; value: number }) {
  return (
    <div className="metric-card">
      <h4>{title}</h4>
      <p>{value}</p>
    </div>
  );
}