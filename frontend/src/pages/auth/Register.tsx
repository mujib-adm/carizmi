import { useForm } from 'react-hook-form';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import '../../styles/pages/auth-form.css';

import { authenticationApi } from '../../api/generated/authentication/authentication';
import { FormField } from '../../component/FormField';
import { MessageBanner } from '../../component/MessageBanner';
import {
  MessageType,
  UserDto
} from '../../api/generated/types';
import { useNotification } from '../../context/NotificationContext';
import { useApiMessages } from '../../hook/ApiResponseHandler';

export default function Register() {
  const { isAuthenticated, isLoading } = useAuth();
  const notify = useNotification();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<UserDto>();
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
          notify.success({ message: 'Success', description: msg.message }, '/login');
        } else {
          handleResponse(responseBody);
        }
      }
    } catch (error: any) {
      handleError(error);
    }
  };

  if (!isLoading && isAuthenticated) return <Navigate to="/" />;

  return (
    <div className="modern-login-container">
      <div className="login-glass-card">
        <form onSubmit={handleSubmit(onSubmit)}>
          <h2>REGISTER</h2>

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

          {globalMessages && <MessageBanner messages={globalMessages} />}

          <button className="global_btn" type="submit">
            REGISTER
          </button>

          <p className="auth-link">
            Already have an account? <a href="/login">Login here</a>
          </p>
        </form>
      </div>
    </div>
  );
}