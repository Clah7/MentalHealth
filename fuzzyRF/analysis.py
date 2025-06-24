import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

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

    # Pilih kolom numerik yang relevan untuk logika fuzzy
    numeric_cols = [
        'Age',
        'Sleep Duration',
        'Quality of Sleep',
        'Heart Rate',
        'Daily Steps'
    ]

    print("--- Menganalisis Distribusi Data ---")
    print("Berikut adalah statistik deskriptif untuk setiap variabel:")
    print(df[numeric_cols].describe())
    print("\n")

    # Atur style plot agar lebih menarik
    sns.set_style("whitegrid")

    # Buat plot histogram untuk setiap kolom
    # figsize disesuaikan agar semua plot muat dengan baik
    fig, axes = plt.subplots(3, 2, figsize=(15, 12))
    fig.suptitle('Distribusi Frekuensi Variabel Numerik', fontsize=16)

    # Meratakan array 'axes' agar mudah di-loop
    axes = axes.flatten()

    for i, col in enumerate(numeric_cols):
        sns.histplot(df[col], kde=True, ax=axes[i], bins=20)
        axes[i].set_title(f'Distribusi {col}')
        axes[i].set_xlabel('Nilai')
        axes[i].set_ylabel('Frekuensi')

    # Menyembunyikan subplot yang tidak terpakai jika jumlahnya ganjil
    if len(numeric_cols) < len(axes):
        for j in range(len(numeric_cols), len(axes)):
            fig.delaxes(axes[j])

    # Mengatur layout agar tidak tumpang tindih
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.show()


# --- Panggil fungsi ini untuk menjalankan analisis ---
if __name__ == "__main__":
    # Jalankan analisis distribusi sebelum melatih model
    analyze_data_distribution()