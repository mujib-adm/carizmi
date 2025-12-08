import api from "../../state/api";
import { useEffect, useState } from "react";
import { Formik, Form, Field, ErrorMessage } from "formik";
import * as Yup from "yup";

const schema = Yup.object({
  firstName: Yup.string().required("Required"),
  lastName: Yup.string().required("Required"),
  phone: Yup.string().matches(/^\(?\d{3}\)?[- ]?\d{3}[- ]?\d{4}$/, "Invalid Phone").required("Required"),
  email: Yup.string().email("Invalid Email").nullable(),
  state: Yup.string().required("Required"),
  zip: Yup.string().required("Required"),
  status: Yup.string().required("Required")
});

export default function Members() {
  const [members, setMembers] = useState<any[]>([]);
  const [selected, setSelected] = useState<any | null>(null);
  const load = async () => {
    const { data } = await api.get("/members");
    setMembers(data.content || data);
  };
  useEffect(() => { load(); }, []);
  return (
    <div className="page members">
      <div className="grid">
        <table>
          <thead><tr><th>Name</th><th>Phone</th><th>Status</th></tr></thead>
          <tbody>
            {members.map(m => (
              <tr key={m.id} onClick={() => setSelected(m)}>
                <td>{m.firstName} {m.lastName}</td>
                <td>{m.phone}</td>
                <td>{m.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="detail">
        <h3>{selected ? "Update Member" : "Add Member"}</h3>
        <Formik
          enableReinitialize
          initialValues={selected || { firstName:"", lastName:"", phone:"", email:"", status:"Active", state:"MN", zip:"", city:"" }}
          validationSchema={schema}
          onSubmit={async (values, { resetForm }) => {
            if (selected) await api.put(`/members/${selected.id}`, values);
            else await api.post("/members", values);
            resetForm(); setSelected(null); load();
          }}
        >
          <Form>
            <label>First Name<Field name="firstName"/></label><ErrorMessage name="firstName" />
            <label>Last Name<Field name="lastName"/></label><ErrorMessage name="lastName" />
            <label>Phone<Field name="phone"/></label><ErrorMessage name="phone" />
            <label>Email<Field name="email"/></label><ErrorMessage name="email" />
            <label>Status<Field as="select" name="status">
              <option>Active</option><option>Disqualified</option><option>Inactive</option>
            </Field></label><ErrorMessage name="status" />
            <label>City<Field name="city"/></label>
            <label>State<Field name="state"/></label><ErrorMessage name="state" />
            <label>Zip<Field name="zip"/></label><ErrorMessage name="zip" />
            <button type="submit">{selected ? "Update" : "Create"}</button>
          </Form>
        </Formik>
      </div>
    </div>
  );
}