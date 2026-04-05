import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import '../../styles/pages/AuthForm.css';

import { authenticationApi } from '../../api/generated/authentication/authentication';
import { FormField } from '../../components/FormField';
import { MessageBanner } from '../../components/MessageBanner';
import { MessageType, UserDto } from '../../api/generated/types';
import { useNotification } from '../../hooks/useNotification';
import { useApiMessages } from '../../hooks/useApiMessages';
import { RoleConstants } from '../../constants/RoleConstants';

const roleOptions = [
  { value: RoleConstants.ROLE_MEMBER, label: 'Member' },
  { value: RoleConstants.ROLE_MANAGER, label: 'Manager' },
  { value: RoleConstants.ROLE_ADMIN, label: 'Admin' },
];

export default function Register() {
  const navigate = useNavigate();
  const notify = useNotification();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<UserDto>({ defaultValues: { role: RoleConstants.ROLE_MEMBER } });
  const { globalMessages, handleResponse, handleError, resetMessages } =
    useApiMessages<UserDto>(setError);

  const onSubmit = async (formValues: UserDto) => {
    try {
      // clear old messages from MessageBanner before starting new call
      resetMessages();
      const response = await authenticationApi.register(formValues);
      const responseBody = response;

      if (responseBody.globalMessages?.length > 0) {
        const msg = responseBody.globalMessages[0];
        if (msg.type === MessageType.SUCCESS) {
          notify.success({ message: 'Success', description: msg.message }, '/users');
        } else {
          handleResponse(responseBody);
        }
      }
    } catch (error: any) {
      handleError(error);
    }
  };

  return (
    <div className="modern-login-container">
      <div className="login-glass-card">
        <form onSubmit={handleSubmit(onSubmit)}>
          <h2>REGISTER NEW USER</h2>

          <div className="modern-field-label">
            First Name <span className="required">*</span>
          </div>
          <FormField
            type="text"
            placeholder="Enter first name..."
            registerProps={register('firstName', { required: 'First Name is required' })}
            error={errors.firstName}
          />

          <div style={{ marginTop: '16px' }}>
            <div className="modern-field-label">
              Last Name <span className="required">*</span>
            </div>
            <FormField
              type="text"
              placeholder="Enter last name..."
              registerProps={register('lastName', { required: 'Last Name is required' })}
              error={errors.lastName}
            />
          </div>

          <div style={{ marginTop: '16px' }}>
            <div className="modern-field-label">
              Email <span className="required">*</span>
            </div>
            <FormField
              type="email"
              placeholder="Enter email..."
              registerProps={register('email', { required: 'Email is required' })}
              error={errors.email}
            />
          </div>

          <div style={{ marginTop: '16px' }}>
            <div className="modern-field-label">
              Username <span className="required">*</span>
            </div>
            <FormField
              type="text"
              placeholder="Enter username..."
              registerProps={register('username', { required: 'Username is required' })}
              error={errors.username}
            />
          </div>

          <div style={{ marginTop: '16px' }}>
            <div className="modern-field-label">
              Password <span className="required">*</span>
            </div>
            <FormField
              type="password"
              placeholder="Enter password..."
              registerProps={register('password', { required: 'Password is required' })}
              error={errors.password}
            />
          </div>

          <div style={{ marginTop: '16px' }}>
            <div className="modern-field-label">
              Role <span className="required">*</span>
            </div>
            <FormField
              as="select"
              options={roleOptions}
              registerProps={register('role', { required: 'Role is required' })}
              error={errors.role}
            />
          </div>

          {globalMessages && <MessageBanner messages={globalMessages} />}

          <button className="global_btn" type="submit">
            REGISTER
          </button>

          <p className="auth-link">
            <a href="/users">← Back to Users</a>
          </p>
        </form>
      </div>
    </div>
  );
}