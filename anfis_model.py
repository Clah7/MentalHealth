import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
import skfuzzy as fuzz
from skfuzzy import control as ctrl
from scipy.optimize import minimize
import warnings
import pickle

warnings.filterwarnings('ignore')


class ANFIS:
    def __init__(self, n_inputs, n_rules=5, learning_rate=0.01, epochs=100):
        self.n_inputs = n_inputs
        self.n_rules = n_rules
        self.learning_rate = learning_rate
        self.epochs = epochs

        # Initialize membership function parameters
        self.mf_params = {}
        for i in range(n_inputs):
            self.mf_params[f'input_{i}'] = {
                'centers': np.linspace(-2, 2, n_rules),
                'widths': np.ones(n_rules) * 0.5
            }

        # Initialize consequent parameters (linear functions)
        self.consequent_params = np.random.randn(n_rules, n_inputs + 1) * 0.1

        self.training_errors = []

    def gaussian_mf(self, x, center, width):
        """Gaussian membership function"""
        return np.exp(-0.5 * ((x - center) / width) ** 2)

    def forward_pass(self, X):
        """Forward pass through ANFIS network"""
        batch_size = X.shape[0]

        # Layer 1: Fuzzification
        membership_values = {}
        for i in range(self.n_inputs):
            membership_values[f'input_{i}'] = np.zeros(
                (batch_size, self.n_rules))
            for j in range(self.n_rules):
                membership_values[f'input_{i}'][:, j] = self.gaussian_mf(
                    X[:, i],
                    self.mf_params[f'input_{i}']['centers'][j],
                    self.mf_params[f'input_{i}']['widths'][j]
                )

        # Layer 2: Rule firing strength
        firing_strength = np.ones((batch_size, self.n_rules))
        for i in range(self.n_inputs):
            firing_strength *= membership_values[f'input_{i}']

        # Layer 3: Normalized firing strength
        firing_strength_sum = np.sum(firing_strength, axis=1, keepdims=True)
        firing_strength_sum = np.where(
            firing_strength_sum == 0, 1e-10, firing_strength_sum)
        normalized_firing_strength = firing_strength / firing_strength_sum

        # Layer 4: Linear output functions
        rule_outputs = np.zeros((batch_size, self.n_rules))
        for i in range(self.n_rules):
            # Linear function: p_i * x1 + q_i * x2 + ... + r_i
            X_extended = np.column_stack([X, np.ones(batch_size)])
            rule_outputs[:, i] = np.dot(X_extended, self.consequent_params[i])

        # Layer 5: Overall output
        output = np.sum(normalized_firing_strength * rule_outputs, axis=1)

        return output, normalized_firing_strength, rule_outputs

    def backward_pass(self, X, y, normalized_firing_strength, rule_outputs, predictions):
        """Backward pass for parameter updating"""
        batch_size = X.shape[0]
        error = predictions - y

        # Update consequent parameters (linear least squares)
        for i in range(self.n_rules):
            if np.sum(normalized_firing_strength[:, i]) > 1e-10:
                X_weighted = X * \
                    normalized_firing_strength[:, i].reshape(-1, 1)
                X_extended = np.column_stack(
                    [X_weighted, normalized_firing_strength[:, i]])
                y_weighted = y * normalized_firing_strength[:, i]

                # Normal equation solution
                try:
                    XTX = np.dot(X_extended.T, X_extended)
                    XTy = np.dot(X_extended.T, y_weighted)
                    if np.linalg.det(XTX) != 0:
                        self.consequent_params[i] = np.linalg.solve(XTX, XTy)
                except:
                    continue

        # Update membership function parameters using gradient descent
        for i in range(self.n_inputs):
            for j in range(self.n_rules):
                # Gradient for center
                grad_center = 0
                grad_width = 0

                for k in range(batch_size):
                    # Calculate partial derivatives
                    mf_val = self.gaussian_mf(
                        X[k, i],
                        self.mf_params[f'input_{i}']['centers'][j],
                        self.mf_params[f'input_{i}']['widths'][j]
                    )

                    if mf_val > 1e-10:
                        # Gradient for center
                        grad_center += error[k] * normalized_firing_strength[k, j] * \
                            rule_outputs[k, j] * mf_val * \
                            (X[k, i] - self.mf_params[f'input_{i}']['centers'][j]) / \
                            (self.mf_params[f'input_{i}']
                             ['widths'][j] ** 2)

                        # Gradient for width
                        grad_width += error[k] * normalized_firing_strength[k, j] * \
                            rule_outputs[k, j] * mf_val * \
                            ((X[k, i] - self.mf_params[f'input_{i}']['centers'][j]) ** 2) / \
                            (self.mf_params[f'input_{i}']
                             ['widths'][j] ** 3)

                # Update parameters
                self.mf_params[f'input_{i}']['centers'][j] -= self.learning_rate * \
                    grad_center / batch_size
                self.mf_params[f'input_{i}']['widths'][j] -= self.learning_rate * \
                    grad_width / batch_size

                # Keep width positive
                self.mf_params[f'input_{i}']['widths'][j] = max(
                    0.1, self.mf_params[f'input_{i}']['widths'][j])

    def fit(self, X, y):
        """Train the ANFIS model"""
        print("Training ANFIS model...")

        for epoch in range(self.epochs):
            # Forward pass
            predictions, normalized_firing_strength, rule_outputs = self.forward_pass(
                X)

            # Calculate error
            mse = mean_squared_error(y, predictions)
            self.training_errors.append(mse)

            # Backward pass
            self.backward_pass(X, y, normalized_firing_strength,
                               rule_outputs, predictions)

            if epoch % 20 == 0:
                print(f"Epoch {epoch}, MSE: {mse:.4f}")

        print("Training completed!")

    def predict(self, X):
        """Make predictions"""
        predictions, _, _ = self.forward_pass(X)
        return predictions

    def plot_training_curve(self):
        """Plot training error curve"""
        plt.figure(figsize=(10, 6))
        plt.plot(self.training_errors)
        plt.title('ANFIS Training Error')
        plt.xlabel('Epoch')
        plt.ylabel('Mean Squared Error')
        plt.grid(True)
        plt.show()


def preprocess_data():
    """Preprocess the dataset"""
    # Create sample data (replace with your actual data loading)
    df = pd.read_csv("dataset.csv")

    # Feature selection
    features = ['Gender', 'Age', 'Sleep Duration', 'Quality of Sleep',
                'BMI Category', 'Heart Rate', 'Daily Steps', 'Sleep Disorder']
    X = df[features].copy()
    y = df['Stress Level'].copy()

    # Data cleaning
    X['BMI Category'] = X['BMI Category'].replace("Normal Weight", "Normal")
    X['Sleep Disorder'] = X['Sleep Disorder'].fillna("Nothing")
    X['Sleep Disorder'] = X['Sleep Disorder'].replace("None", "Nothing")

    # Encode categorical variables
    X['Gender'] = X['Gender'].map({'Male': 0, 'Female': 1})
    X['BMI Category'] = X['BMI Category'].map(
        {'Normal': 0, 'Overweight': 1, 'Obese': 2})
    X['Sleep Disorder'] = X['Sleep Disorder'].map(
        {'Nothing': 0, 'Sleep Apnea': 1, 'Insomnia': 2})

    return X, y


def evaluate_model(y_true, y_pred):
    """Evaluate model performance"""
    mse = mean_squared_error(y_true, y_pred)
    mae = mean_absolute_error(y_true, y_pred)
    r2 = r2_score(y_true, y_pred)

    print(f"\nModel Performance:")
    print(f"Mean Squared Error (MSE): {mse:.4f}")
    print(f"Mean Absolute Error (MAE): {mae:.4f}")
    print(f"R² Score: {r2:.4f}")

    return mse, mae, r2


def plot_predictions(y_true, y_pred, title="ANFIS Predictions"):
    """Plot actual vs predicted values"""
    plt.figure(figsize=(10, 6))
    plt.scatter(y_true, y_pred, alpha=0.7)
    plt.plot([y_true.min(), y_true.max()], [
             y_true.min(), y_true.max()], 'r--', lw=2)
    plt.xlabel('Actual Stress Level')
    plt.ylabel('Predicted Stress Level')
    plt.title(title)
    plt.grid(True)
    plt.show()


# Main execution
if __name__ == "__main__":
    # Load and preprocess data
    print("Loading and preprocessing data...")
    X, y = preprocess_data()

    # Note: For your actual dataset, use:
    # df = pd.read_csv('dataset.csv')
    # Then apply the same preprocessing steps

    print(f"Dataset shape: {X.shape}")
    print(f"Features: {list(X.columns)}")

    # Split data
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    # Standardize features
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    # Create and train ANFIS model
    anfis = ANFIS(
        n_inputs=X_train_scaled.shape[1],
        n_rules=3,  # Adjust based on your data complexity
        learning_rate=0.01,
        epochs=100
    )

    # Train the model
    anfis.fit(X_train_scaled, y_train.values)

    # Make predictions
    y_train_pred = np.clip(np.round(anfis.predict(X_train_scaled)), 1, 10)
    y_test_pred = np.clip(np.round(anfis.predict(X_test_scaled)), 1, 10)

    # Evaluate performance
    print("\n=== Training Set Performance ===")
    evaluate_model(y_train, y_train_pred)

    print("\n=== Test Set Performance ===")
    evaluate_model(y_test, y_test_pred)

    # Plot results
    plot_predictions(y_train, y_train_pred, "ANFIS Training Set Predictions")
    plot_predictions(y_test, y_test_pred, "ANFIS Test Set Predictions")

    # Plot training curve
    anfis.plot_training_curve()

    # Example prediction for new data
    print("\n=== Example Prediction ===")
    # Create a sample input (Male, Age 30, Sleep Duration 7, Quality 8, Normal BMI, HR 70, Steps 8000, No disorder)
    sample_input = np.array([[0, 100, 7.0, 8, 0, 70, 10000, 0]])
    sample_input_scaled = scaler.transform(sample_input)
    prediction = anfis.predict(sample_input_scaled)
    rounded_prediction = np.clip(np.round(prediction[0]), 1, 10).astype(int)
    print(f"Predicted stress level for sample input: {rounded_prediction}")

    # Save model and scaler to .pkl file
    with open("anfis_model.pkl", "wb") as f:
        pickle.dump({"model": anfis, "scaler": scaler}, f)
    print("Model saved as anfis_model.pkl")
