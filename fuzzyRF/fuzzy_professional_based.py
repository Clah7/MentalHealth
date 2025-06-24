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

warnings.filterwarnings('ignore')  # Suppress warnings


def load_model():
    df = pd.read_csv("dataset.csv")
    # X = df[['Gender', 'Age', 'Sleep Duration', 'Quality of Sleep', 'Physical Activity Level', 'BMI Category', 'Heart Rate', 'Daily Steps', 'Sleep Disorder']]
    X = df[['Gender', 'Age', 'Sleep Duration', 'Quality of Sleep',
            'BMI Category', 'Heart Rate', 'Daily Steps', 'Sleep Disorder']]
    X['BMI Category'] = X['BMI Category'].replace("Normal Weight", "Normal")
    X['Sleep Disorder'] = X['Sleep Disorder'].fillna("Nothing")
    y = df['Stress Level']

    encoders = {}

    le_gender = LabelEncoder()
    X['Gender'] = le_gender.fit_transform(X['Gender'])
    encoders['Gender'] = le_gender

    le_bmi = LabelEncoder()
    X['BMI Category'] = le_bmi.fit_transform(X['BMI Category'])
    encoders['BMI Category'] = le_bmi

    le_sleep_disorder = LabelEncoder()
    X['Sleep Disorder'] = le_sleep_disorder.fit_transform(X['Sleep Disorder'])
    encoders['Sleep Disorder'] = le_sleep_disorder

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42)
    model = RandomForestClassifier(random_state=42)
    model.fit(X_train, y_train)
    accuracy = accuracy_score(y_test, model.predict(X_test))

    return model, encoders, accuracy


def create_fuzzy_features(df_input):
    # Definisikan universe of discourse untuk setiap fitur numerik
    # Rentang ini bisa disesuaikan berdasarkan analisis data Anda
    age = ctrl.Antecedent(np.arange(18, 70, 1), 'Age')
    sleep_duration = ctrl.Antecedent(np.arange(0, 12, 0.1), 'Sleep Duration')
    quality_of_sleep = ctrl.Antecedent(np.arange(0, 11, 1), 'Quality of Sleep')
    heart_rate = ctrl.Antecedent(np.arange(40, 120, 1), 'Heart Rate')
    daily_steps = ctrl.Antecedent(np.arange(0, 15000, 100), 'Daily Steps')

    # Definisikan fungsi keanggotaan (membership functions)
    # 1. Usia (Age) - Kategori Demografi Umum
    age = ctrl.Antecedent(np.arange(18, 71, 1), 'Age')
    age['young'] = fuzz.trimf(age.universe, [18, 28, 40])
    age['middle'] = fuzz.trimf(age.universe, [35, 50, 65])
    age['old'] = fuzz.trimf(age.universe, [60, 70, 70])

    # 2. Durasi Tidur (Sleep Duration) - National Sleep Foundation
    sleep_duration = ctrl.Antecedent(np.arange(0, 12.1, 0.1), 'Sleep Duration')
    sleep_duration['short'] = fuzz.trimf(sleep_duration.universe, [0, 5.5, 7.0])
    sleep_duration['adequate'] = fuzz.trimf(sleep_duration.universe, [6.5, 8.0, 9.5])
    sleep_duration['long'] = fuzz.trimf(sleep_duration.universe, [9.0, 10.5, 12])

    # 3. Kualitas Tidur (Quality of Sleep) - Pembagian Logis Skala 1-10
    quality_of_sleep = ctrl.Antecedent(np.arange(0, 11, 1), 'Quality of Sleep')
    quality_of_sleep['poor'] = fuzz.trimf(quality_of_sleep.universe, [0, 2, 5])
    quality_of_sleep['average'] = fuzz.trimf(quality_of_sleep.universe, [4, 6, 8])
    quality_of_sleep['excellent'] = fuzz.trimf(quality_of_sleep.universe, [7, 9, 10])

    # 4. Denyut Jantung (Heart Rate) - American Heart Association
    heart_rate = ctrl.Antecedent(np.arange(40, 121, 1), 'Heart Rate')
    heart_rate['low'] = fuzz.trimf(heart_rate.universe, [40, 50, 60])
    heart_rate['normal'] = fuzz.trimf(heart_rate.universe, [60, 80, 100])
    heart_rate['high'] = fuzz.trimf(heart_rate.universe, [100, 110, 120])

    # 5. Jumlah Langkah Harian (Daily Steps) - Studi Aktivitas Fisik (Tudor-Locke)
    daily_steps = ctrl.Antecedent(np.arange(0, 20001, 100), 'Daily Steps')
    daily_steps['low'] = fuzz.trimf(daily_steps.universe, [0, 2500, 5000])
    daily_steps['moderate'] = fuzz.trimf(daily_steps.universe, [5000, 8000, 10000])
    daily_steps['high'] = fuzz.trimf(daily_steps.universe, [9000, 12000, 20000])

    fuzzy_features_list = []
    for index, row in df_input.iterrows():
        # Pastikan nilai numerik ada sebelum fuzzyfication
        current_age = row['Age']
        current_sleep_duration = row['Sleep Duration']
        current_quality_of_sleep = row['Quality of Sleep']
        current_heart_rate = row['Heart Rate']
        current_daily_steps = row['Daily Steps']

        # Hitung derajat keanggotaan
        age_young_m = fuzz.interp_membership(
            age.universe, age['young'].mf, current_age)
        age_middle_m = fuzz.interp_membership(
            age.universe, age['middle'].mf, current_age)
        age_old_m = fuzz.interp_membership(
            age.universe, age['old'].mf, current_age)

        sd_short_m = fuzz.interp_membership(
            sleep_duration.universe, sleep_duration['short'].mf, current_sleep_duration)
        sd_adequate_m = fuzz.interp_membership(
            sleep_duration.universe, sleep_duration['adequate'].mf, current_sleep_duration)
        sd_long_m = fuzz.interp_membership(
            sleep_duration.universe, sleep_duration['long'].mf, current_sleep_duration)

        qos_poor_m = fuzz.interp_membership(
            quality_of_sleep.universe, quality_of_sleep['poor'].mf, current_quality_of_sleep)
        qos_average_m = fuzz.interp_membership(
            quality_of_sleep.universe, quality_of_sleep['average'].mf, current_quality_of_sleep)
        qos_excellent_m = fuzz.interp_membership(
            quality_of_sleep.universe, quality_of_sleep['excellent'].mf, current_quality_of_sleep)

        hr_low_m = fuzz.interp_membership(
            heart_rate.universe, heart_rate['low'].mf, current_heart_rate)
        hr_normal_m = fuzz.interp_membership(
            heart_rate.universe, heart_rate['normal'].mf, current_heart_rate)
        hr_high_m = fuzz.interp_membership(
            heart_rate.universe, heart_rate['high'].mf, current_heart_rate)

        ds_low_m = fuzz.interp_membership(
            daily_steps.universe, daily_steps['low'].mf, current_daily_steps)
        ds_moderate_m = fuzz.interp_membership(
            daily_steps.universe, daily_steps['moderate'].mf, current_daily_steps)
        ds_high_m = fuzz.interp_membership(
            daily_steps.universe, daily_steps['high'].mf, current_daily_steps)

        fuzzy_features_list.append({
            'Age_young_M': age_young_m, 'Age_middle_M': age_middle_m, 'Age_old_M': age_old_m,
            'SD_short_M': sd_short_m, 'SD_adequate_M': sd_adequate_m, 'SD_long_M': sd_long_m,
            'QoS_poor_M': qos_poor_m, 'QoS_average_M': qos_average_m, 'QoS_excellent_M': qos_excellent_m,
            'HR_low_M': hr_low_m, 'HR_normal_M': hr_normal_m, 'HR_high_M': hr_high_m,
            'DS_low_M': ds_low_m, 'DS_moderate_M': ds_moderate_m, 'DS_high_M': ds_high_m
        })
    return pd.DataFrame(fuzzy_features_list)


def load_model_fuzzy():
    try:
        df = pd.read_csv("dataset.csv")
        print("--- Dataset Berhasil Dimuat (5 baris pertama) ---")
        print(df.head())
        print(f"\nJumlah baris: {len(df)}")
        print("\n")
    except FileNotFoundError:
        print("Error: File 'dataset.csv' tidak ditemukan. Pastikan file berada di direktori yang sama atau berikan path lengkap.")
        return None, None, None

    # Pra-pemrosesan awal
    # Pilih fitur yang akan digunakan, sesuai dengan definisi Anda
    features_to_use = ['Gender', 'Age', 'Sleep Duration', 'Quality of Sleep',
                       'BMI Category', 'Heart Rate', 'Daily Steps', 'Sleep Disorder']
    # Gunakan .copy() untuk menghindari SettingWithCopyWarning
    X = df[features_to_use].copy()
    y = df['Stress Level'].copy()

    # Data Cleaning / Preprocessing seperti yang Anda definisikan
    X['BMI Category'] = X['BMI Category'].replace("Normal Weight", "Normal")
    X['Sleep Disorder'] = X['Sleep Disorder'].fillna("Nothing")

    # Pastikan kolom numerik yang akan di-fuzzy-kan adalah tipe numerik
    X['Age'] = pd.to_numeric(X['Age'], errors='coerce')
    X['Sleep Duration'] = pd.to_numeric(X['Sleep Duration'], errors='coerce')
    X['Quality of Sleep'] = pd.to_numeric(
        X['Quality of Sleep'], errors='coerce')
    X['Heart Rate'] = pd.to_numeric(X['Heart Rate'], errors='coerce')
    X['Daily Steps'] = pd.to_numeric(X['Daily Steps'], errors='coerce')

    # Tangani NaN yang mungkin muncul setelah to_numeric (jika ada)
    # Mengisi NaN dengan median sebagai strategi default
    for col in ['Age', 'Sleep Duration', 'Quality of Sleep', 'Heart Rate', 'Daily Steps']:
        if X[col].isnull().any():
            median_val = X[col].median()
            X[col] = X[col].fillna(median_val)
            print(f"Mengisi NaN di '{col}' dengan median: {median_val}")

    # Buat fitur fuzzy
    fuzzy_df = create_fuzzy_features(X)
    print("--- Fitur Fuzzy Berhasil Dibuat (5 baris pertama) ---")
    print(fuzzy_df.head())
    print("\n")

    # Gabungkan fitur fuzzy dengan fitur asli (numerik dan yang akan di-encode)
    # Kita akan mempertahankan fitur numerik asli DAN fitur fuzzy-nya
    X_hybrid = pd.concat([X, fuzzy_df], axis=1)

    # Encoding fitur kategorikal
    encoders = {}
    categorical_cols = ['Gender', 'BMI Category', 'Sleep Disorder']
    for col in categorical_cols:
        le = LabelEncoder()
        X_hybrid[col] = le.fit_transform(X_hybrid[col])
        encoders[col] = le

    # Encoding target Stress Level
    le_stress = LabelEncoder()
    y_encoded = le_stress.fit_transform(y)
    encoders['Stress Level'] = le_stress  # Simpan encoder untuk target juga

    print("--- Data Hybrid Lengkap Setelah Encoding (5 baris pertama) ---")
    print(X_hybrid.head())
    print(
        f"\nTotal kolom setelah fuzzyfication dan encoding: {X_hybrid.shape[1]}")
    print("\n")

    # Split data
    X_train, X_test, y_train, y_test = train_test_split(
        X_hybrid, y_encoded, test_size=0.2, random_state=42)

    # Build and train the RandomForestClassifier
    model = RandomForestClassifier(random_state=42)
    model.fit(X_train, y_train)

    # Evaluate the model
    y_pred = model.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    print(
        f'Accuracy of the Hybrid Stress Level Predictor (Fuzzy + RF): {accuracy:.2f}')

    # Confusion Matrix
    cm = confusion_matrix(y_test, y_pred)
    plt.figure(figsize=(10, 8))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
                xticklabels=le_stress.classes_, yticklabels=le_stress.classes_)
    plt.title('Confusion Matrix (Hybrid Model)')
    plt.xlabel('Predicted Label')
    plt.ylabel('True Label')
    plt.show()

    return model, encoders, accuracy


def predict_stress_level_fuzzy(data, model, encoders):
    # Buat DataFrame dari data input tunggal
    df_input = pd.DataFrame([data])

    # Pra-pemrosesan data input tunggal (sesuai dengan load_model_fuzzy)
    df_input['BMI Category'] = df_input['BMI Category'].replace(
        "Normal Weight", "Normal")
    df_input['Sleep Disorder'] = df_input['Sleep Disorder'].fillna("Nothing")

    # Pastikan tipe data numerik
    df_input['Age'] = pd.to_numeric(df_input['Age'], errors='coerce')
    df_input['Sleep Duration'] = pd.to_numeric(
        df_input['Sleep Duration'], errors='coerce')
    df_input['Quality of Sleep'] = pd.to_numeric(
        df_input['Quality of Sleep'], errors='coerce')
    df_input['Heart Rate'] = pd.to_numeric(
        df_input['Heart Rate'], errors='coerce')
    df_input['Daily Steps'] = pd.to_numeric(
        df_input['Daily Steps'], errors='coerce')

    # Tangani NaN pada data input tunggal (jika ada)
    for col in ['Age', 'Sleep Duration', 'Quality of Sleep', 'Heart Rate', 'Daily Steps']:
        if df_input[col].isnull().any():
            # Untuk prediksi, jika ada NaN, gunakan median dari data pelatihan
            # Ini memerlukan akses ke median yang dihitung saat pelatihan.
            # Untuk kesederhanaan, kita bisa menggunakan 0 atau melempar error
            # atau menyimpan median dari df pelatihan di 'encoders'
            # Ganti dengan strategi yang lebih baik jika ada NaN di input prediksi
            df_input[col] = df_input[col].fillna(0)
            print(
                f"Peringatan: NaN ditemukan di '{col}' pada input prediksi, diisi dengan 0.")

    # Buat fitur fuzzy untuk data input tunggal
    fuzzy_input_df = create_fuzzy_features(df_input)

    # Gabungkan fitur fuzzy dengan fitur asli yang sudah diproses
    # Pastikan urutan kolom sama seperti saat pelatihan
    X_input_processed = pd.concat([df_input, fuzzy_input_df], axis=1)

    # Encoding fitur kategorikal pada data input tunggal
    categorical_cols = ['Gender', 'BMI Category', 'Sleep Disorder']
    for col in categorical_cols:
        if col in encoders:  # Pastikan encoder ada
            X_input_processed[col] = encoders[col].transform(
                X_input_processed[col])
        else:
            print(f"Peringatan: Encoder untuk '{col}' tidak ditemukan.")

    # Pastikan kolom X_input_processed sama dengan X_train
    # Ini sangat penting! Jika ada kolom yang hilang atau berlebih, prediksi akan gagal.
    # Kita perlu memastikan X_input_processed hanya memiliki kolom yang ada di X_train
    # dan dalam urutan yang sama.
    # Cara paling aman adalah membuat ulang df_input dengan kolom X_train
    # dan mengisi nilai-nilai dari data input.
    # Mendapatkan nama kolom yang digunakan saat pelatihan
    X_train_cols = model.feature_names_in_
    final_input_df = pd.DataFrame(columns=X_train_cols)
    for col in X_train_cols:
        if col in X_input_processed.columns:
            final_input_df[col] = X_input_processed[col]
        else:
            # Jika ada kolom yang hilang (misalnya, fitur fuzzy yang tidak terpicu)
            # Isi dengan 0 atau nilai default yang sesuai
            # Default untuk derajat keanggotaan fuzzy
            final_input_df[col] = 0.0

    # Make a prediction
    prediction_encoded = model.predict(final_input_df)[0]

    # Decode the prediction back to original stress level
    stress_level_encoder = encoders['Stress Level']
    prediction = stress_level_encoder.inverse_transform([prediction_encoded])[
        0]
    return prediction


# --- MAIN EXECUTION ---
if __name__ == "__main__":
    model_fuzzy, encoders_fuzzy, accuracy_fuzzy = load_model_fuzzy()

    if model_fuzzy is not None:
        print(
            f"\nModel Hybrid berhasil dimuat dengan akurasi: {accuracy_fuzzy:.2f}")

        # Contoh penggunaan fungsi predict_stress_level_fuzzy
        # Data input untuk prediksi (harus sesuai dengan format kolom asli)
        sample_data = {
            'Gender': 'Male',
            'Age': 30,
            'Sleep Duration': 7.5,
            'Quality of Sleep': 8,
            'BMI Category': 'Normal',
            'Heart Rate': 70,
            'Daily Steps': 8000,
            'Sleep Disorder': 'Nothing'
        }

        predicted_stress = predict_stress_level_fuzzy(
            sample_data, model_fuzzy, encoders_fuzzy)
        print(
            f"\nPrediksi Tingkat Stres untuk data contoh: {predicted_stress}")

        # Contoh data lain
        sample_data_2 = {
            'Gender': 'Female',
            'Age': 45,
            'Sleep Duration': 5.0,
            'Quality of Sleep': 4,
            'BMI Category': 'Obese',
            'Heart Rate': 90,
            'Daily Steps': 3000,
            'Sleep Disorder': 'Insomnia'
        }
        predicted_stress_2 = predict_stress_level_fuzzy(
            sample_data_2, model_fuzzy, encoders_fuzzy)
        print(
            f"Prediksi Tingkat Stres untuk data contoh 2: {predicted_stress_2}")

    # Anda bisa membandingkan dengan model tanpa fuzzy jika ingin
    # Panggil fungsi load_model asli Anda
    model_pure, encoders_pure, accuracy_pure = load_model()
    print(
        f"\nModel Pure RF (tanpa fuzzy) dimuat dengan akurasi: {accuracy_pure:.2f}")
