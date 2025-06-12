import joblib
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score, confusion_matrix
import numpy as np
import skfuzzy as fuzz
from skfuzzy import control as ctrl
import matplotlib.pyplot as plt
import seaborn as sns
import warnings
from sklearn.metrics import classification_report
from sklearn.model_selection import cross_val_score

warnings.filterwarnings('ignore')  # Suppress warnings

# ----------------------------------------------------------------------------------
# FUNGSI UTILITAS UNTUK ANALISIS DISTRIBUSI (seperti sebelumnya, untuk referensi)
# ----------------------------------------------------------------------------------
def analyze_data_distribution(file_path="dataset.csv"):
    """
    Fungsi untuk memuat dataset dan menampilkan histogram
    dari variabel numerik utama untuk analisis fuzzy.
    """
    try:
        df = pd.read_csv(file_path)
    except FileNotFoundError:
        print(f"Error: File '{file_path}' tidak ditemukan.")
        return

    numeric_cols = ['Age', 'Sleep Duration', 'Quality of Sleep', 'Heart Rate', 'Daily Steps']
    print("--- Menganalisis Distribusi Data ---")
    print(df[numeric_cols].describe())
    sns.set_style("whitegrid")
    fig, axes = plt.subplots(3, 2, figsize=(15, 12))
    fig.suptitle('Distribusi Frekuensi Variabel Numerik', fontsize=16)
    axes = axes.flatten()

    for i, col in enumerate(numeric_cols):
        sns.histplot(df[col], kde=True, ax=axes[i], bins=20)
        axes[i].set_title(f'Distribusi {col}')
        axes[i].set_xlabel('Nilai')
        axes[i].set_ylabel('Frekuensi')

    if len(numeric_cols) < len(axes):
        for j in range(len(numeric_cols), len(axes)):
            fig.delaxes(axes[j])
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.show()

# ----------------------------------------------------------------------------------
# KODE INTI YANG TELAH DIMODIFIKASI SECARA PENUH
# ----------------------------------------------------------------------------------

def create_fuzzy_features(df_input):
    """
    Fungsi ini telah diperbarui sepenuhnya berdasarkan analisis histogram data Anda.
    Baik definisi, rentang (universe), nama kategori, maupun proses fuzzifikasi telah disesuaikan.
    """
    # ------------------ Definisi Antecedent Disesuaikan Berdasarkan Histogram Data Anda ------------------

    # 1. Usia (Age) - Disesuaikan dengan rentang data, kategori 'old' tidak relevan
    age = ctrl.Antecedent(np.arange(25, 61, 1), 'Age')
    age['young'] = fuzz.trimf(age.universe, [25, 33, 40])
    age['middle'] = fuzz.trimf(age.universe, [38, 44, 59])

    # 2. Durasi Tidur (Sleep Duration) - Konfirmasi, definisi awal sudah baik
    sleep_duration = ctrl.Antecedent(np.arange(5.5, 9.1, 0.1), 'Sleep Duration')
    sleep_duration['short'] = fuzz.trimf(sleep_duration.universe, [5.5, 6.2, 7.0])
    sleep_duration['adequate'] = fuzz.trimf(sleep_duration.universe, [6.8, 8.0, 9.0])
    # Kategori 'long' bisa dipertahankan untuk kasus edge, meskipun jarang di data Anda
    sleep_duration['long'] = fuzz.trimf(sleep_duration.universe, [8.8, 9.0, 9.0])

    # 3. Kualitas Tidur (Quality of Sleep) - Disesuaikan minor
    quality_of_sleep = ctrl.Antecedent(np.arange(4, 11, 1), 'Quality of Sleep')
    quality_of_sleep['poor'] = fuzz.trimf(quality_of_sleep.universe, [4, 4, 6])
    quality_of_sleep['average'] = fuzz.trimf(quality_of_sleep.universe, [5, 6.5, 8])
    quality_of_sleep['excellent'] = fuzz.trimf(quality_of_sleep.universe, [7, 9, 10])

    # 4. Denyut Jantung (Heart Rate) - Disesuaikan signifikan ("Zoom In" pada rentang normal)
    heart_rate = ctrl.Antecedent(np.arange(65, 87, 1), 'Heart Rate')
    heart_rate['normal_low'] = fuzz.trimf(heart_rate.universe, [65, 68, 72])
    heart_rate['normal_mid'] = fuzz.trimf(heart_rate.universe, [70, 75, 80])
    heart_rate['normal_high'] = fuzz.trimf(heart_rate.universe, [78, 82, 86])

    # 5. Jumlah Langkah Harian (Daily Steps) - Disesuaikan dengan klaster data
    daily_steps = ctrl.Antecedent(np.arange(3000, 10001, 100), 'Daily Steps')
    daily_steps['low'] = fuzz.trimf(daily_steps.universe, [3000, 4500, 6000])
    daily_steps['moderate'] = fuzz.trimf(daily_steps.universe, [5500, 7800, 9000])
    daily_steps['high'] = fuzz.trimf(daily_steps.universe, [8500, 9500, 10000])

    # ------------------ Proses Fuzzifikasi dengan Nama Kategori Baru ------------------
    fuzzy_features_list = []
    for index, row in df_input.iterrows():
        # Input nilai dari baris data
        current_age = row['Age']
        current_sleep_duration = row['Sleep Duration']
        current_quality_of_sleep = row['Quality of Sleep']
        current_heart_rate = row['Heart Rate']
        current_daily_steps = row['Daily Steps']

        # Fuzzifikasi Usia (Age) - Kategori 'old' dihilangkan
        age_young_m = fuzz.interp_membership(age.universe, age['young'].mf, current_age)
        age_middle_m = fuzz.interp_membership(age.universe, age['middle'].mf, current_age)
        
        # Fuzzifikasi Durasi Tidur (Sleep Duration)
        sd_short_m = fuzz.interp_membership(sleep_duration.universe, sleep_duration['short'].mf, current_sleep_duration)
        sd_adequate_m = fuzz.interp_membership(sleep_duration.universe, sleep_duration['adequate'].mf, current_sleep_duration)
        sd_long_m = fuzz.interp_membership(sleep_duration.universe, sleep_duration['long'].mf, current_sleep_duration)

        # Fuzzifikasi Kualitas Tidur (Quality of Sleep)
        qos_poor_m = fuzz.interp_membership(quality_of_sleep.universe, quality_of_sleep['poor'].mf, current_quality_of_sleep)
        qos_average_m = fuzz.interp_membership(quality_of_sleep.universe, quality_of_sleep['average'].mf, current_quality_of_sleep)
        qos_excellent_m = fuzz.interp_membership(quality_of_sleep.universe, quality_of_sleep['excellent'].mf, current_quality_of_sleep)
        
        # Fuzzifikasi Denyut Jantung (Heart Rate) - Menggunakan nama kategori baru
        hr_normal_low_m = fuzz.interp_membership(heart_rate.universe, heart_rate['normal_low'].mf, current_heart_rate)
        hr_normal_mid_m = fuzz.interp_membership(heart_rate.universe, heart_rate['normal_mid'].mf, current_heart_rate)
        hr_normal_high_m = fuzz.interp_membership(heart_rate.universe, heart_rate['normal_high'].mf, current_heart_rate)

        # Fuzzifikasi Jumlah Langkah (Daily Steps)
        ds_low_m = fuzz.interp_membership(daily_steps.universe, daily_steps['low'].mf, current_daily_steps)
        ds_moderate_m = fuzz.interp_membership(daily_steps.universe, daily_steps['moderate'].mf, current_daily_steps)
        ds_high_m = fuzz.interp_membership(daily_steps.universe, daily_steps['high'].mf, current_daily_steps)

        # Membuat dictionary dengan key yang telah diperbarui
        fuzzy_features_list.append({
            # Age: 'Age_old_M' dihilangkan
            'Age_young_M': age_young_m, 'Age_middle_M': age_middle_m,
            # Sleep Duration: Tetap
            'SD_short_M': sd_short_m, 'SD_adequate_M': sd_adequate_m, 'SD_long_M': sd_long_m,
            # Quality of Sleep: Tetap
            'QoS_poor_M': qos_poor_m, 'QoS_average_M': qos_average_m, 'QoS_excellent_M': qos_excellent_m,
            # Heart Rate: Key disesuaikan dengan nama kategori baru
            'HR_normal_low_M': hr_normal_low_m, 'HR_normal_mid_M': hr_normal_mid_m, 'HR_normal_high_M': hr_normal_high_m,
            # Daily Steps: Tetap
            'DS_low_M': ds_low_m, 'DS_moderate_M': ds_moderate_m, 'DS_high_M': ds_high_m
        })
        
    return pd.DataFrame(fuzzy_features_list)

def load_model_fuzzy():
    try:
        df = pd.read_csv("dataset.csv")
    except FileNotFoundError:
        print("Error: File 'dataset.csv' tidak ditemukan.")
        return None, None, None

    features_to_use = ['Gender', 'Age', 'Sleep Duration', 'Quality of Sleep',
                       'BMI Category', 'Heart Rate', 'Daily Steps', 'Sleep Disorder']
    X = df[features_to_use].copy()
    y = df['Stress Level'].copy()
    
    X['BMI Category'] = X['BMI Category'].replace("Normal Weight", "Normal")
    X['Sleep Disorder'] = X['Sleep Disorder'].fillna("Nothing")

    numeric_cols_to_check = ['Age', 'Sleep Duration', 'Quality of Sleep', 'Heart Rate', 'Daily Steps']
    for col in numeric_cols_to_check:
        X[col] = pd.to_numeric(X[col], errors='coerce')
        if X[col].isnull().any():
            median_val = X[col].median()
            X[col] = X[col].fillna(median_val)

    # Panggil fungsi create_fuzzy_features yang sudah diperbarui
    fuzzy_df = create_fuzzy_features(X)

    X_hybrid = pd.concat([X.reset_index(drop=True), fuzzy_df.reset_index(drop=True)], axis=1)

    encoders = {}
    categorical_cols = ['Gender', 'BMI Category', 'Sleep Disorder']
    for col in categorical_cols:
        le = LabelEncoder()
        X_hybrid[col] = le.fit_transform(X_hybrid[col])
        encoders[col] = le

    le_stress = LabelEncoder()
    y_encoded = le_stress.fit_transform(y)
    encoders['Stress Level'] = le_stress

    X_train, X_test, y_train, y_test = train_test_split(
        X_hybrid, y_encoded, test_size=0.2, random_state=42)

    model = RandomForestClassifier(random_state=42)
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    print(f'Akurasi dari Hybrid Stress Level Predictor (Fuzzy-Data-Driven + RF): {accuracy:.4f}')

    # --- TAMBAHKAN KODE EVALUASI DI SINI ---
    print("\n--- Laporan Klasifikasi Rinci ---")
    print(classification_report(y_test, y_pred))

    cm = confusion_matrix(y_test, y_pred)
    plt.figure(figsize=(10, 8))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
                xticklabels=le_stress.classes_, yticklabels=le_stress.classes_)
    plt.title('Confusion Matrix (Hybrid Model - Data Driven)')
    plt.xlabel('Predicted Label')
    plt.ylabel('True Label')
    plt.show()

    joblib.dump(model, 'model/stress_model.pkl')
    joblib.dump(encoders, 'model/encoders.pkl')

    return model, encoders, accuracy

def predict_stress_level_fuzzy(data, model, encoders):
    df_input = pd.DataFrame([data])
    df_input['BMI Category'] = df_input['BMI Category'].replace("Normal Weight", "Normal")
    df_input['Sleep Disorder'] = df_input['Sleep Disorder'].fillna("Nothing")
    
    numeric_cols_to_check = ['Age', 'Sleep Duration', 'Quality of Sleep', 'Heart Rate', 'Daily Steps']
    for col in numeric_cols_to_check:
        df_input[col] = pd.to_numeric(df_input[col], errors='coerce').fillna(0)

    fuzzy_input_df = create_fuzzy_features(df_input)
    X_input_processed = pd.concat([df_input.reset_index(drop=True), fuzzy_input_df.reset_index(drop=True)], axis=1)

    categorical_cols = ['Gender', 'BMI Category', 'Sleep Disorder']
    for col in categorical_cols:
        if col in encoders:
            X_input_processed[col] = encoders[col].transform(X_input_processed[col])
    
    # Memastikan kolom input sama persis dengan kolom saat training
    X_train_cols = model.feature_names_in_
    final_input_df = pd.DataFrame(columns=X_train_cols)
    final_input_df = pd.concat([final_input_df, X_input_processed], ignore_index=True).fillna(0)
    final_input_df = final_input_df[X_train_cols] # Menjaga urutan dan kelengkapan kolom
    
    prediction_encoded = model.predict(final_input_df)[0]
    prediction = encoders['Stress Level'].inverse_transform([prediction_encoded])[0]
    
    return prediction

def evaluate_with_cross_validation():
    """
    Fungsi untuk mengevaluasi model menggunakan K-Fold Cross-Validation
    untuk mendapatkan skor performa yang lebih andal.
    """
    print("\n--- Mengevaluasi Model dengan 5-Fold Cross-Validation ---")
    
    # 1. Muat dan siapkan data (langkah-langkah yang sama seperti di load_model_fuzzy)
    try:
        df = pd.read_csv("dataset.csv")
    except FileNotFoundError:
        print("Error: File 'dataset.csv' tidak ditemukan.")
        return

    features_to_use = ['Gender', 'Age', 'Sleep Duration', 'Quality of Sleep',
                       'BMI Category', 'Heart Rate', 'Daily Steps', 'Sleep Disorder']
    X = df[features_to_use].copy()
    y = df['Stress Level'].copy()
    
    X['BMI Category'] = X['BMI Category'].replace("Normal Weight", "Normal")
    X['Sleep Disorder'] = X['Sleep Disorder'].fillna("Nothing")

    numeric_cols_to_check = ['Age', 'Sleep Duration', 'Quality of Sleep', 'Heart Rate', 'Daily Steps']
    for col in numeric_cols_to_check:
        X[col] = pd.to_numeric(X[col], errors='coerce')
        if X[col].isnull().any():
            X[col] = X[col].fillna(X[col].median())

    fuzzy_df = create_fuzzy_features(X)
    X_hybrid = pd.concat([X.reset_index(drop=True), fuzzy_df.reset_index(drop=True)], axis=1)

    encoders = {}
    categorical_cols = ['Gender', 'BMI Category', 'Sleep Disorder']
    for col in categorical_cols:
        le = LabelEncoder()
        X_hybrid[col] = le.fit_transform(X_hybrid[col])
        encoders[col] = le

    le_stress = LabelEncoder()
    y_encoded = le_stress.fit_transform(y)

    # 2. Definisikan model
    model = RandomForestClassifier(random_state=42)

    # 3. Lakukan Cross-Validation
    # cv=5 berarti menggunakan 5-fold cross-validation
    scores = cross_val_score(model, X_hybrid, y_encoded, cv=5, scoring='accuracy')

    # 4. Tampilkan hasilnya
    print(f"Skor akurasi untuk setiap fold: {scores}")
    print(f"Rata-rata Akurasi (Mean Accuracy): {scores.mean():.4f}")
    print(f"Standar Deviasi Akurasi: {scores.std():.4f}")

    print("\nRata-rata akurasi ini adalah estimasi yang lebih realistis tentang bagaimana model akan bekerja pada data baru.")

# --- MAIN EXECUTION ---
if __name__ == "__main__":
    
    # Opsional: jalankan analisis ini sekali untuk melihat plot
    # analyze_data_distribution()

    # Latih model dengan logika fuzzy yang baru dan disesuaikan
    model_fuzzy, encoders_fuzzy, accuracy_fuzzy = load_model_fuzzy()

    if model_fuzzy is not None:
        print("\n--- Prediksi dengan Model Hybrid Baru ---")

        # Contoh data 1 (sesuai dengan rentang data)
        sample_data = {
            'Gender': 'Male',
            'Age': 33,
            'Sleep Duration': 7.5,
            'Quality of Sleep': 8,
            'BMI Category': 'Normal',
            'Heart Rate': 75,
            'Daily Steps': 8000,
            'Sleep Disorder': 'Nothing'
        }
        predicted_stress = predict_stress_level_fuzzy(sample_data, model_fuzzy, encoders_fuzzy)
        print(f"\nPrediksi Tingkat Stres untuk data contoh 1: {predicted_stress}")

        # Contoh data 2 (nilai disesuaikan agar masuk dalam universe baru)
        sample_data_2 = {
            'Gender': 'Female',
            'Age': 45,
            'Sleep Duration': 7.0, # Disesuaikan dari 5.0 -> 6.0 (masuk universe 5.5-9.1)
            'Quality of Sleep': 4,  # Disesuaikan dari 4 -> 6
            'BMI Category': 'Normal',
            'Heart Rate': 85,       # Disesuaikan dari 90 -> 85 (masuk universe 65-87)
            'Daily Steps': 10000,    # Disesuaikan dari 3000 -> 5500
            'Sleep Disorder': 'Nothing'
        }
        predicted_stress_2 = predict_stress_level_fuzzy(sample_data_2, model_fuzzy, encoders_fuzzy)
        print(f"Prediksi Tingkat Stres untuk data contoh 2: {predicted_stress_2}")

        evaluate_with_cross_validation()
        